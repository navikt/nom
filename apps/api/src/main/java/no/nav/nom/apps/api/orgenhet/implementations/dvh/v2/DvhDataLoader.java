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
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetIdGenerator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.AvvikleOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.KorrigerOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.OpprettOrgEnhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.postgresql.EventBasedOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.postgresql.OrgenhetHendelseRepository;
import no.nav.nom.apps.api.postgresql.GenericStorageElement;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
    private final OrgEnhetIdGenerator orgEnhetIdGenerator;


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

        val hendelserToSave = new LinkedList<IHendelse>();
        val fidMarkedForEndDate = new HashMap<String, LocalDate>();
        val fidChangedStartDate = new HashMap<String, LocalDate>();

        parsedEnheter.forEach((key, value) -> value.stream().sorted(Comparator.comparing(IOrgenhet::getGyldigFom).reversed()).forEach(parsedEnhet -> {

            // Eventsourcing block
            if (unleash.isEnabled(UNLEASH_TOGGLE)) {
                val stateEnheterPerUniqueAgressoId = orgEnhetStateService.findAllByUniqueAgressoId(parsedEnhet.getUniqueAgressoId());
                val stateEnheterMatchingDvhParsedEnhet = stateEnheterPerUniqueAgressoId.stream()
                        .filter(p ->
                                isOverlapping(parsedEnhet,
                                        fidChangedStartDate.containsKey(p.fid()) ? fidChangedStartDate.get(p.fid()) : p.gyldigFom(),
                                        fidMarkedForEndDate.containsKey(p.fid()) ? fidMarkedForEndDate.get(p.fid()) : p.gyldigTom().orElse(null))
                                || withinOneMonth(parsedEnhet,
                                        fidChangedStartDate.containsKey(p.fid()) ? fidChangedStartDate.get(p.fid()) : p.gyldigFom(),
                                        fidMarkedForEndDate.containsKey(p.fid()) ? fidMarkedForEndDate.get(p.fid()) : p.gyldigTom().orElse(null))
                                )
                        .toList();

                if (stateEnheterMatchingDvhParsedEnhet.size() < 2) {
                    stateEnheterMatchingDvhParsedEnhet.stream().findAny().ifPresentOrElse( orgEnhetState -> {
                        // Parsed DVH enhet found in state
                        if (parsedEnhet.getGyldigTom().isPresent() && orgEnhetState.gyldigTom().isEmpty()) {
                            avvikleOrgEnhet(orgEnhetState.fid(), parsedEnhet);
                            // Need to keep track of Eventual gyldigTom in case there are more parsed enheter for same uniqueAgressoId to process
                            fidMarkedForEndDate.put(orgEnhetState.fid(), parsedEnhet.getGyldigTom().get());
                        }
                        // Ved overlapp skal orgEnhetState korrigeres med fra-til dato settes likt DvhEnhet fra-til - Hvis disse er forskjellig
                        // TODO: EndreLeder og EndrePlassering

                        korrigerOrgEnhetHvisEndringer(parsedEnhet, orgEnhetState);
                        // Need to keep track of Eventual changed gyldigFom in case there are more parsed enheter for same uniqueAgressoId to process
                        if (!parsedEnhet.getGyldigFom().equals(orgEnhetState.gyldigFom())) {
                            fidChangedStartDate.put(orgEnhetState.fid(), parsedEnhet.getGyldigFom());
                        }
                    }, () -> {
                        // Parsed DVH enhet NOT found in state
                        String fid = opprettOrgEnhet(parsedEnhet);
                        if (parsedEnhet.getGyldigTom().isPresent()) {
                            avvikleOrgEnhet(fid, parsedEnhet);
                        }
                    });
                } else {
                    log.error("Manual correction needed, enhet merge error with fids {} for parsed DVH enhet [{}]",
                            stateEnheterMatchingDvhParsedEnhet.stream().map(OrgEnhetState::fid).toList(), parsedEnhet.getUniqueAgressoId());
                }
            }

            val x = persistedEnheterOld.getOrDefault(parsedEnhet.getUniqueAgressoId(), Collections.emptyList());
            val matchingEnheter = x.stream().filter(p -> isOverlapping(parsedEnhet, p) || withinOneMonth(parsedEnhet, p)).toList();

            if (matchingEnheter.size() < 2) {
                matchingEnheter.stream().findAny().ifPresentOrElse((p) -> {
                    val endreHendelse = createEndreHendelse(parsedEnhet, p, getOpprettetAv());
                    if (endreHendelse != null) {
                        hendelserToSave.add(endreHendelse);
                        ((EventBasedOrgenhet) p).addHendelse(endreHendelse);
                    }
                }, () -> {
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


    public boolean isOverlapping(IOrgenhet orgenhetV2One, IOrgenhet orgenhetV2Two) {
        return orgenhetV2One.getGyldigFom().isBefore(orgenhetV2Two.getGyldigTom().orElse(LocalDate.MAX)) && orgenhetV2Two.getGyldigFom()
                .isBefore(orgenhetV2One.getGyldigTom().orElse(LocalDate.MAX));
    }

    public boolean isOverlapping(DvhOrgenhetV2 dvhOrgenhet, LocalDate fromDate, LocalDate toDate) {
        toDate = toDate != null ? toDate : LocalDate.MAX;
        return dvhOrgenhet.getGyldigFom().isBefore(toDate) && fromDate
                .isBefore(dvhOrgenhet.getGyldigTom().orElse(LocalDate.MAX));
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

    public boolean withinOneMonth(DvhOrgenhetV2 dvhOrgenhet, LocalDate fromDate, LocalDate toDate) {
        toDate = toDate != null ? toDate : LocalDate.MAX;
        return (
            Math.abs(ChronoUnit.MONTHS.between(
                    dvhOrgenhet.getGyldigFom().withDayOfMonth(1),
                    toDate
                )) <= 1
            || Math.abs(ChronoUnit.MONTHS.between(
                    fromDate,
                    dvhOrgenhet.getGyldigTom().orElse(LocalDate.MAX).withDayOfMonth(1)
                )) <= 1);
    }

    private String opprettOrgEnhet(DvhOrgenhetV2 dvhOrgenhetV2) {
        final var opprettOrgEnhet = new OpprettOrgEnhet(
                orgEnhetIdGenerator.newId(),
                dvhOrgenhetV2.getNavn(),
                dvhOrgenhetV2.getGyldigFom(),
                Optional.empty(),
                Optional.empty(),
                StaticOrgenhetkategoriMapping.resolveFor(dvhOrgenhetV2.getAgressoId(), dvhOrgenhetV2.getOrgNiv()),
            new InputMetadata("nom-api BatchJob", "Datavarehus", dvhOrgenhetV2.getRawData(), new Origination("nom-api", "nom-api"))
        );
        return orgEnhetCommandService.opprettOrgEnhet(opprettOrgEnhet);
    }

    private void avvikleOrgEnhet(String fid, DvhOrgenhetV2 dvhOrgenhetV2) {
        final var avvikleOrgEnhet = new AvvikleOrgEnhet(
                dvhOrgenhetV2.getGyldigTom().get(),
                new InputMetadata("nom-api BatchJob", "Datavarehus", dvhOrgenhetV2.getRawData(), new Origination("nom-api", "nom-api"))
        );
        orgEnhetCommandService.avikleOrgEnhet(fid, avvikleOrgEnhet);
    }

    // Hvis det er forskjell mellom dvh og state, send korrigering
    // Gjelder ikke for sluttdato null -> dato satt. Det er AvvikleOrgEnhet
    // Eller endring på leder. Det er OrgEnhetEndreLeder
    // Eller endring på overordnet orgEnhet. Det er OrgEnhetEndreOverordnetEnhet
    private void korrigerOrgEnhetHvisEndringer(DvhOrgenhetV2 dvhOrgenhetV2, OrgEnhetState orgEnhetState) {
        val navnEndret = !orgEnhetState.navn().equals(dvhOrgenhetV2.getNavn());
        val datoFOMEndret = !orgEnhetState.gyldigFom().equals(dvhOrgenhetV2.getGyldigFom());
        val datoTOMEndret = orgEnhetState.gyldigTom().isPresent() && !dvhOrgenhetV2.getGyldigTom().equals(orgEnhetState.gyldigTom());
        val kategoriEndret = !orgEnhetState.orgEnhetsKategori().equals(Optional.ofNullable(StaticOrgenhetkategoriMapping.resolveFor(dvhOrgenhetV2.getAgressoId(), dvhOrgenhetV2.getOrgNiv())));
        if (navnEndret || datoFOMEndret || datoTOMEndret || kategoriEndret) {
            val korrigering = new KorrigerOrgEnhet(
                    orgEnhetState.id(),
                    dvhOrgenhetV2.getNavn(),
                    dvhOrgenhetV2.getGyldigFom(),
                    dvhOrgenhetV2.getGyldigTom(),
                    orgEnhetState.overordnetOrgEnhetFid(),
                    orgEnhetState.lederFid(),
                    StaticOrgenhetkategoriMapping.resolveFor(dvhOrgenhetV2.getAgressoId(), dvhOrgenhetV2.getOrgNiv()),
                    new InputMetadata("nom-api BatchJob", "Datavarehus", dvhOrgenhetV2.getRawData(), new Origination("nom-api", "nom-api"))
            );
            orgEnhetCommandService.korrigerOrgEnhet(orgEnhetState.fid(), korrigering);
        }
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