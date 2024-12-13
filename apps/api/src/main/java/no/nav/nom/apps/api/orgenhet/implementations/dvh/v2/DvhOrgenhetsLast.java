package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.getunleash.Unleash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.common.NomProxyRestClient;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetIdGenerator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.AvvikleOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.KorrigerOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.OpprettOrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.job.OTUtenforHierarki;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DvhOrgenhetsLast {
    private final ObjectMapper objectMapper;
    private final ValidationService validationService;
    private final NomProxyRestClient nomProxyRestClient;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgEnhetCommandService orgEnhetCommandService;
    private final OrgEnhetIdGenerator orgEnhetIdGenerator;

    private final OTUtenforHierarki otUtenforHierarki;
    private final Unleash unleash;
    private final DvhOrdsService dvhOrdsService;

    /*
    1. Enhter blir avsluttet hos oss ved at maxstillingsdato settes. sluttdato settes ikke.
    2. ØT puncher feil og oppretter ny stilling på enheten etter den er avsluttet.
    3. Man kan gjenoppstarte enheten HVIS det opprettes ny rad i agresso innenfor 1mnd av den gamle
     */
    public void lastOrgenhetsDataFraDvhTilEvtsrc() {
        log.info("Laster orgenhet v2 fra dvh");
        try {
            List<DvhOrgenhetV2> dvhOrgenheter;
            if (unleash.isEnabled("nom.dvh.last_orgenheter_fra_ords")) {
                dvhOrgenheter = dvhOrdsService.getOrgenheter();
            } else {
                dvhOrgenheter = nomProxyRestClient.getTdvOrgenheter();
            }

            getOrdsOrgenheterAndCompare(dvhOrgenheter);

            val validations = Arrays.stream(ValidationType.values()).collect(Collectors.toMap(Function.identity(), a -> new LinkedList<DvhOrgenhetV2>()));

            val parsedEnheter = dvhOrgenheter.stream()
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

            log.info("Orgenhet v2 last fra dvh. Parsed antall enheter:{}", parsedEnheter.size());

            debugLogDuplikaterPaaUniqueAgressoId(parsedEnheter);

            parsedEnheter.forEach((key, value) -> value.stream().sorted(Comparator.comparing(IOrgenhet::getGyldigFom).reversed())
                .forEach(parsedEnhet -> {
                    val stateEnheterPerUniqueAgressoId = orgEnhetStateRepository.findAllByUniqueAgressoId(parsedEnhet.getUniqueAgressoId());
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
                        // LF - dersom det var mer enn 1 mnd mellom skulle det lages som to separate enheter, selv om navnet var likt.
                        // Ref - https://jira.adeo.no/browse/ORG-1755
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
            .forEach(orgenhetV2 -> {
                    val stateEnheterPerUniqueAgressoId = orgEnhetStateRepository.findAllByUniqueAgressoId(orgenhetV2.getUniqueAgressoId());
                    stateEnheterPerUniqueAgressoId
                        .stream()
                        .filter(orgEnhetState -> orgEnhetState.gyldigTom().map(gyldigTom -> gyldigTom.isBefore(LocalDate.now())).orElse(false))
                        .forEach(orgEnhetState -> {
                            LocalDate gyldigTom = orgenhetV2.getGyldigTom().orElse(LocalDate.now());
                            if (!orgEnhetState.gyldigTom().get().equals(gyldigTom)) {
                                avvikleOrgEnhet(orgEnhetState.fid(), orgenhetV2, gyldigTom);
                            }
                        });
                }
            );

        validations.get(ValidationType.PARKED_INVALID_PERIOD)
            .forEach(orgenhetV2 -> {
                    val stateEnheterPerUniqueAgressoId = orgEnhetStateRepository.findAllByUniqueAgressoId(orgenhetV2.getUniqueAgressoId());
                    stateEnheterPerUniqueAgressoId
                        .stream()
                        .filter(orgEnhetState -> orgEnhetState.gyldigTom().map(gyldigTom -> gyldigTom.isBefore(LocalDate.now())).orElse(false))
                        .forEach(orgEnhetState -> avvikleOrgEnhet(orgEnhetState.fid(), orgenhetV2, LocalDate.now()));
                }
            );
            log.info("Ferdig med orgenhet last");

            // Finn OTer utenfor hierarki
            otUtenforHierarki.findOTUtenforHierarki();
        } catch (Exception e) {
            log.error("Failure loading data from DVH", e);
        }
    }

    private void getOrdsOrgenheterAndCompare(List<DvhOrgenhetV2> tdvDvhOrgenheter) {
        try {
            var ordsDvhOrgenheter = dvhOrdsService.getOrgenheter();

            boolean erLikt = true;
            if (ordsDvhOrgenheter.size() != tdvDvhOrgenheter.size()) {
                log.error("Antall orgenheter fra TDV og ORDS er ulik. TDV: {}, ORDS: {}", tdvDvhOrgenheter.size(), ordsDvhOrgenheter.size());
                erLikt = false;
            }

            var ordsMap = ordsDvhOrgenheter.stream().collect(Collectors.toMap(this::getOrgenhetUniqueKey, Function.identity()));
            int idxSkip = tdvDvhOrgenheter.size()/200;
            for (int i = 0; i < 200; i++) {
                int idx = i * idxSkip;
                var dvhOrgenhetTdv = tdvDvhOrgenheter.get(idx);
                var dvhOrgenhetOrds = ordsMap.get(getOrgenhetUniqueKey(dvhOrgenhetTdv));
                if (!DvhOrgenhetV2.compare(dvhOrgenhetTdv, dvhOrgenhetOrds)) {
                    log.error("Orgenhet fra TDV og ORDS er ulik. \n\tTDV: \n\t{}, \n\tORDS: \n\t{}"
                        , dvhOrgenhetTdv.getRawData().toPrettyString()
                        , dvhOrgenhetOrds.getRawData().toPrettyString());
                    erLikt = false;
                }
            }
            if (erLikt) {
                log.info("Last av orgenheter fra ORDS (NOM_ORDS_HRORG_ORGENHET.json) er lik last fra TDV (NOM_HRORG_ORGENHET)");
            } else {
                log.error("Last av orgenheter fra ORDS (NOM_ORDS_HRORG_ORGENHET.json) er annerledes enn last fra TDV (NOM_HRORG_ORGENHET)");
            }
        } catch (Exception e) {
            log.error("Feilet i last av orgenheter fra ORDS (NOM_ORDS_HRORG_ORGENHET.json)", e);
        }
    }

    private String getOrgenhetUniqueKey(DvhOrgenhetV2 dvhOrgenhetV2) {
        return dvhOrgenhetV2.getUniqueAgressoId() + "-" + dvhOrgenhetV2.getOrgenhetStatus() + "-" + dvhOrgenhetV2.getGyldigFom();
    }

    private void debugLogDuplikaterPaaUniqueAgressoId(Map<String, List<DvhOrgenhetV2>> parsedEnheter) {
        log.debug("logger evt. duplikater på UniqueAgressoId");
        parsedEnheter.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .forEach(e -> {
                    StringBuffer sb = new StringBuffer();
                    sb.append("Duplikater på UniqueAgressoId: ").append(e.getKey()).append("\n");
                    e.getValue().forEach(dvhOrgenhetV2 -> sb.append("   ").append(dvhOrgenhetV2.getRawData()).append("\n"));
                    log.debug(sb.toString());
                });
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
        val agressoOrgEnhetTypeEndret = !Objects.equals(orgEnhetState.agressoOrgenhetType().orElse(null), dvhOrgenhetV2.getAgressoOrgenhetType());

        if (navnEndret || datoFOMEndret || datoTOMEndret ||agressoOrgEnhetTypeEndret) {
            val korrigering = new KorrigerOrgEnhet(
                    orgEnhetState.id(),
                    dvhOrgenhetV2.getNavn(),
                    dvhOrgenhetV2.getGyldigFom(),
                    dvhOrgenhetV2.getGyldigTom(),
                    orgEnhetState.overordnetOrgEnhetFid(),
                    orgEnhetState.lederFid(),
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