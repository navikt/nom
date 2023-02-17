package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.NomProxyRestClient;
import no.nav.nom.apps.api.common.security.AuthenticationProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.hendelse.contract.IHendelseService;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetIdGenerator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.AvvikleOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.KorrigerOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.OpprettOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class DvhDataLoader {
    private final TokenRepository tokenRepository;
    private final ObjectMapper objectMapper;
    private final IHendelseService hendelseService;
    private final AuthenticationProperties authenticationProperties;
    private final ValidationService validationService;
    private final NomProxyRestClient nomProxyRestClient;
    private final OrgenhetService orgenhetService;
    private final OrgEnhetStateRepository orgEnhetStateService;
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

        val fidMarkedForEndDate = new HashMap<String, LocalDate>();
        val fidChangedStartDate = new HashMap<String, LocalDate>();

        parsedEnheter.forEach((key, value) -> value.stream().sorted(Comparator.comparing(IOrgenhet::getGyldigFom).reversed())
            .forEach(parsedEnhet -> {
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
            })
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

        // Parkerte orgenheter som har ikke stillinger ble filtrert bort, men skal fjernes fra State om de er der fra før
        validations.get(ValidationType.PARKED_WITHOUT_STILLINGER)
            .stream()
            .filter(dvhOrgenhetV2 -> dvhOrgenhetV2.getGyldigTom().map(gyldigTom -> gyldigTom.isBefore(LocalDate.now())).orElse(false))
            .forEach(orgenhetV2 -> {
                    val stateEnheterPerUniqueAgressoId = orgEnhetStateService.findAllByUniqueAgressoId(orgenhetV2.getUniqueAgressoId());
                    stateEnheterPerUniqueAgressoId.forEach(orgEnhetState -> {

                        LocalDate gyldigTom = orgenhetV2.getGyldigTom().orElse(LocalDate.now());
                        avvikleOrgEnhet(orgEnhetState.fid(), orgenhetV2, gyldigTom);
                    });
                }
            );

        validations.get(ValidationType.PARKED_INVALID_PERIOD)
            .stream()
            .filter(dvhOrgenhetV2 -> dvhOrgenhetV2.getGyldigTom().map(gyldigTom -> gyldigTom.isBefore(LocalDate.now())).orElse(false))
            .forEach(orgenhetV2 -> {
                    val stateEnheterPerUniqueAgressoId = orgEnhetStateService.findAllByUniqueAgressoId(orgenhetV2.getUniqueAgressoId());
                    stateEnheterPerUniqueAgressoId.forEach(orgEnhetState -> {

                        avvikleOrgEnhet(orgEnhetState.fid(), orgenhetV2, LocalDate.now());
                    });
                }
            );

        log.info("Ferdig med orgenhet last");
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
        avvikleOrgEnhet(fid, dvhOrgenhetV2, dvhOrgenhetV2.getGyldigTom().get());
    }

    private void avvikleOrgEnhet(String fid, DvhOrgenhetV2 dvhOrgenhetV2, LocalDate gyldigTom) {
        final var avvikleOrgEnhet = new AvvikleOrgEnhet(
            gyldigTom,
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
}