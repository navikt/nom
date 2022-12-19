package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.getunleash.Unleash;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.NomProxyRestClient;
import no.nav.nom.apps.api.common.security.AuthenticationProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.hendelse.contract.HendelseDataType;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadata;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
import no.nav.nom.apps.api.hendelse.contract.IHendelse;
import no.nav.nom.apps.api.hendelse.contract.IHendelseService;
import no.nav.nom.apps.api.orgenhet.OrgEnhetStateService;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.AvvikleOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.OpprettOrgEnhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.postgresql.EventBasedOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.postgresql.OrgenhetHendelseRepository;
import no.nav.nom.apps.api.postgresql.GenericStorageElement;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@Import(no.nav.nom.libs.utils.common.unleash.Config.class)
public class DvhDataLoader {

    public static final String UNLEASH_TOGGLE = "nom.orgenhet.eventsourcing";
    private final Unleash unleash;

    private final TokenRepository tokenRepository;
    private final ObjectMapper objectMapper;
    private final IHendelseService hendelseService;
    private final AuthenticationProperties authenticationProperties;
    private final ValidationService validationService;
    private final NomProxyRestClient nomProxyRestClient;
    private final OrgenhetService orgenhetService;
    private final OrgEnhetStateService orgEnhetStateService;
    private final OrgenhetHendelseRepository orgenhetHendelseRepository;
    private final OrgEnhetCommandService orgEnhetCommandService;


    /*
    1. Enhter blir avsluttet hos oss ved at maxstillingsdato settes. sluttdato settes ikke.
    2. ØT puncher feil og oppretter ny stilling på enheten etter den er avsluttet.
    3. Man kan gjenoppstarte enheten HVIS det opprettes ny rad i agresso innenfor 1mnd av den gamle
     */
    public void load() {
        log.info("Laster orgenhet v2 fra dvh");
        val response = nomProxyRestClient.getEnheterResponse();

        val validations = Arrays.stream(ValidationType.values()).collect(Collectors.toMap(Function.identity(), a -> new LinkedList<DvhOrgenhetV2>()));

        val parsedEnheter = StreamSupport.stream(response.spliterator(), false) // 8496 enheter
                .map(DvhOrgenhetV2::new)
                // Validation: Orgenhetstatus should only be P and N
                .filter(e -> {
                    if (!e.getOrgenhetStatus().equals("P") && !e.getOrgenhetStatus().equals("N")) {
                        validations.get(ValidationType.ORGENHET_STATUS).add(e);
                        return false;
                    }
                    return true;
                })
                // Validation: Enhet is invalid if its parked and never had stillinger.
                .filter(e -> {
                    if (e.getOrgNiv().equals("ORGENHET")) {
                        if (e.getOrgenhetStatus().equals("P")) {
                            if (e.getStillingMaksDato() == null) {
                                validations.get(ValidationType.PARKED_WITHOUT_STILLINGER).add(e);
                                return false;
                            }
                        }
                    }
                    return true;
                }).filter(e -> {
                    if (e.getOrgNiv().equals("ORGENHET")) {
                        if (e.getOrgenhetStatus().equals("P") && e.getGyldigFom().isAfter(e.getGyldigTom().orElse(LocalDate.MAX))) {
                            validations.get(ValidationType.PARKED_INVALID_PERIOD).add(e);
                            return false;
                        }
                    }
                    return true;
                })
                // Grupperer enheter på kombinasjonen agressoId + navn + agresso orgnivå
                .collect(Collectors.groupingBy(DvhOrgenhetV2::getUniqueAgressoId)).entrySet().stream()
                // Slår sammen enheter der gyldighetsperiodene er skilt med 1 mnd eller mindre
                .peek(entry -> {
                    val list = entry.getValue();
                    if (list.size() > 1) {
                        for (int i = list.size() - 1; i >= 0; i--) {
                            val enhetLast = list.get(i);
                            for (int j = 0; j < i; j++) {
                                val enhetCompare = list.get(j);
                                boolean shouldMerge = withinOneMonth(enhetLast, enhetCompare) || isOverlapping(enhetLast, enhetCompare);
                                if (shouldMerge) {
                                    enhetCompare.merge(enhetLast, objectMapper);
                                    list.remove(enhetLast);
                                }
                            }
                        }
                    }
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        val persistedEnheterOld = orgenhetService.getAllEnheter().stream().collect(Collectors.groupingBy(IOrgenhet::getUniqueAgressoId));

        val persistedEnheterNewStream = orgEnhetStateService.getAll().stream().collect(Collectors.groupingBy(OrgEnhetState::uniqueAgressoId));

        val hendelserToSave = new LinkedList<IHendelse>();


        parsedEnheter.forEach((key, value) -> value.stream().sorted(Comparator.comparing(IOrgenhet::getGyldigFom).reversed()).forEach(parsedEnhet -> {
            val x = persistedEnheterOld.getOrDefault(parsedEnhet.getUniqueAgressoId(), Collections.emptyList());
            val matchingEnheter = x.stream().filter(p -> isOverlapping(parsedEnhet, p) || withinOneMonth(parsedEnhet, p)).toList();

            if (matchingEnheter.size() < 2) {
                matchingEnheter.stream().findAny().ifPresentOrElse((p) -> {
                    if (unleash.isEnabled(UNLEASH_TOGGLE)) {// Eventsourcing block
                        // TODO: Håndter Korrigeringer, EndreLeder og EndrePlassering
                        if (parsedEnhet.getGyldigTom().isPresent() && !parsedEnhet.getGyldigTom().equals(p.getGyldigTom())) {
                            avvikleOrgEnhet(p.getFid().toString(), parsedEnhet); // TODO:!! Use FID, not UUID.STRING When state is from eventSource
                        }
                    }
                    val endreHendelse = createEndreHendelse(parsedEnhet, p, getOpprettetAv());
                    if (endreHendelse != null) {
                        hendelserToSave.add(endreHendelse);
                        ((EventBasedOrgenhet) p).addHendelse(endreHendelse);
                    }
                }, () -> {
                    if (unleash.isEnabled(UNLEASH_TOGGLE)) { // Eventsourcing block
                        String fid = opprettOrgEnhet(parsedEnhet);
                        if (parsedEnhet.getGyldigTom().isPresent()) {
                            avvikleOrgEnhet(fid, parsedEnhet);
                        }
                    }
                    hendelserToSave.add(createOpprettHendelse(parsedEnhet, getOpprettetAv()));
                });
            } else {
                log.error("Manual correction needed, enhet merge error with fids " + matchingEnheter.stream().map(IOrgenhet::getFid).toList());
            }
        }));


        orgenhetHendelseRepository.saveAll(
                hendelserToSave.stream()
                        .filter(Objects::nonNull)
                        .map(e -> GenericStorageElement.builder()
                                .id(e.getId())
                                .document(objectMapper.convertValue(e, JsonNode.class))
                                .storedDate(LocalDateTime.now())
                                .dataType(GenericStorageElement.DataType.HENDELSE_V1)
                                .build())
                        .toList()
        );


        validationService.saveValidations(
                validations.entrySet()
                        .stream()
                        .map(s -> s.getValue()
                                .stream()
                                .map(v -> Validation.builder()
                                        .validationCode(s.getKey().name())
                                        .validationDescription(s.getKey().beskrivelse)
                                        .rawInputData(v.getRawData())
                                        .build())
                                .toList())
                        .flatMap(List::stream)
                        .toList()
        );
    }

    private boolean isNewEnhet(IOrgenhet persistedEnhet, IOrgenhet newEnhet) {
        return persistedEnhet == null || !isOverlapping(persistedEnhet, newEnhet) || !withinOneMonth(persistedEnhet, newEnhet);
    }

    public boolean isOverlapping(IOrgenhet orgenhetV2One, IOrgenhet orgenhetV2Two) {
        return orgenhetV2One.getGyldigFom().isBefore(orgenhetV2Two.getGyldigTom().orElse(LocalDate.MAX)) && orgenhetV2Two.getGyldigFom()
                .isBefore(orgenhetV2One.getGyldigTom().orElse(LocalDate.MAX));
    }

    public boolean withinOneMonth(IOrgenhet dvhOrgenhetV2One, IOrgenhet dvhOrgenhetV2Two) {
        return dvhOrgenhetV2One != dvhOrgenhetV2Two && (Math.abs(ChronoUnit.MONTHS.between(
                dvhOrgenhetV2One.getGyldigFom().withDayOfMonth(1),
                dvhOrgenhetV2Two.getGyldigTom()
                        .orElse(LocalDate.MAX)
                        .withDayOfMonth(1)
        )) <= 1 || Math.abs(ChronoUnit.MONTHS.between(
                dvhOrgenhetV2Two.getGyldigFom().withDayOfMonth(1),
                dvhOrgenhetV2One.getGyldigTom().orElse(LocalDate.MAX).withDayOfMonth(1)
        )) <= 1);
    }

    private String opprettOrgEnhet(DvhOrgenhetV2 dvhOrgenhetV2) {
        final var opprettOrgEnhet = new OpprettOrgEnhet(
                "Intern_id_" + dvhOrgenhetV2.getAgressoId(), // TODO:!! Create NOM id - ikke bruk agressoId
                dvhOrgenhetV2.getNavn(),
                dvhOrgenhetV2.getGyldigFom(),
                Optional.empty(),
                Optional.empty(),
                StaticOrgenhetkategoriMapping.resolveFor(dvhOrgenhetV2.getAgressoId(), dvhOrgenhetV2.getOrgNiv()),
            new InputMetadata("nom-api BatchJob", "Datavarehus", dvhOrgenhetV2.getRawData(), new Origination("nom-api", "nom-api"))
        );
        return orgEnhetCommandService.createOrgEnhet(opprettOrgEnhet);
    }

    private void avvikleOrgEnhet(String fid, DvhOrgenhetV2 dvhOrgenhetV2) {
        final var avvikleOrgEnhet = new AvvikleOrgEnhet(
                dvhOrgenhetV2.getGyldigTom().get(),
                new InputMetadata("nom-api BatchJob", "Datavarehus", dvhOrgenhetV2.getRawData(), new Origination("nom-api", "nom-api"))
        );
        orgEnhetCommandService.terminateOrgEnhet(fid, avvikleOrgEnhet);
    }

    @SneakyThrows
    private IHendelse createOpprettHendelse(DvhOrgenhetV2 dvhOrgenhetV2, HendelseMetadata.OpprettetAv opprettetAv) {
        val hendelse = objectMapper.createObjectNode();
        hendelse.put(EventBasedOrgenhet.NAVN_FIELD, dvhOrgenhetV2.getNavn());
        hendelse.put(EventBasedOrgenhet.GYLDIG_FOM_FIELD, dvhOrgenhetV2.getGyldigFom().toString());
        if (dvhOrgenhetV2.getGyldigTom().isPresent()) {
            hendelse.put(EventBasedOrgenhet.GYLDIG_TOM_FIELD, dvhOrgenhetV2.getGyldigTom().get().toString());
        } else {
            hendelse.putNull(EventBasedOrgenhet.GYLDIG_TOM_FIELD);
        }
        hendelse.put(EventBasedOrgenhet.AGRESSO_ID_FIELD, dvhOrgenhetV2.getAgressoId());
        hendelse.put(EventBasedOrgenhet.AGRESSO_ORGNIV_FIELD, dvhOrgenhetV2.getOrgNiv());
        hendelse.put(EventBasedOrgenhet.AGRESSO_ORGENHET_TYPE_FIELD, dvhOrgenhetV2.getAgressoOrgenhetType());


        return hendelseService.createOpprettHendelse(
                hendelse,
                HendelseMetadataSerialize.builder()
                        .opprettetAv(opprettetAv)
                        .opprettetTidspunkt(LocalDateTime.now())
                        .rawInputData(dvhOrgenhetV2.getRawData())
                        .build(),
                HendelseDataType.ORGENHET_V1
        );
    }

    @SneakyThrows
    private IHendelse createEndreHendelse(DvhOrgenhetV2 dvhOrgenhetV2, IOrgenhet eventBasedOrgenhet, HendelseMetadata.OpprettetAv opprettetAv) {
        val hendelse = objectMapper.createObjectNode();
        if (!dvhOrgenhetV2.getGyldigFom().equals(eventBasedOrgenhet.getGyldigFom())) {
            hendelse.put(EventBasedOrgenhet.GYLDIG_FOM_FIELD, dvhOrgenhetV2.getGyldigFom().toString());
        }
        if (!Objects.equals(dvhOrgenhetV2.getGyldigTom().orElse(null), eventBasedOrgenhet.getGyldigTom().orElse(null))) {
            hendelse.put(EventBasedOrgenhet.GYLDIG_TOM_FIELD, dvhOrgenhetV2.getGyldigTom().isPresent() ? dvhOrgenhetV2.getGyldigTom().get().toString() : null);
        }
        if (!Objects.equals(dvhOrgenhetV2.getAgressoOrgenhetType(), eventBasedOrgenhet.getAgressoOrgenhetType())) {
            hendelse.put(EventBasedOrgenhet.AGRESSO_ORGENHET_TYPE_FIELD, dvhOrgenhetV2.getAgressoOrgenhetType());
        }

        if (!hendelse.isEmpty()) {
            return hendelseService.createEndreHendelse(
                    eventBasedOrgenhet.getFid(),
                    hendelse,
                    HendelseMetadataSerialize.builder()
                            .opprettetAv(opprettetAv)
                            .opprettetTidspunkt(LocalDateTime.now())
                            .rawInputData(dvhOrgenhetV2.getRawData())
                            .build(),
                    HendelseDataType.ORGENHET_V1
            );
        }

        return null;
    }

    public enum ValidationType {
        HELFO("Navn inneholder HELFO"),
        ORGENHET_STATUS("Ugyldig orgenhetstatus"),
        PARKED_WITHOUT_STILLINGER("Enheten er parkert uten stillinger"),
        PARKED_INVALID_PERIOD("Parkert enhet med gyldigTom som er før gyldigFom");

        String beskrivelse;

        ValidationType(String beskrivelse) {
            this.beskrivelse = beskrivelse;
        }
    }

    @SneakyThrows
    private HendelseMetadata.OpprettetAv getOpprettetAv() {
        val jwt = tokenRepository.getSystemAadToken("api://" + authenticationProperties.aad().clientId() + "/.default");
        return new HendelseMetadata.OpprettetAv(jwt.getJWTClaimsSet().getClaim("azp_name").toString(), jwt.getJWTClaimsSet().getClaim("azp_name").toString());
    }
}