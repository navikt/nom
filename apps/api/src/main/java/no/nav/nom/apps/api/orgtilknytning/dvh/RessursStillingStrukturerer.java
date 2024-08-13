package no.nav.nom.apps.api.orgtilknytning.dvh;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode.DagligOppfolging;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static no.nav.nom.apps.api.common.AppConfig.SECURE_LOGS;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.NO_END_DATE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.isCurrentPeriod;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.isHistoricPeriod;

@Component
@Slf4j
public class RessursStillingStrukturerer {

    public static final String UNLEASH_RESSURS_STILLING_STRUKTURER_FIKS_INKONSISTENS = "nom.ressurs-stilling-strukturer.fiks_inkonsistens";

    private final OrgenhetService orgenhetService;
    private final RessursStillingLoadStatus ressursStillingLoadStatus;
    private final RessursStateRepository ressursStateRepository;
    private final Environment environment;
    private final Unleash unleash;

    public RessursStillingStrukturerer(
        OrgenhetService orgenhetService, RessursStillingLoadStatus ressursStillingLoadStatus,
        RessursStateRepository ressursStateRepository,
        Environment environment,
        Unleash unleash
    ) {
        this.orgenhetService = orgenhetService;
        this.ressursStillingLoadStatus = ressursStillingLoadStatus;
        this.ressursStateRepository = ressursStateRepository;
        this.environment = environment;
        this.unleash = unleash;
    }

    public Map<String, List<DvhRessursStilling>> grupperPerNavident(List<DvhRessursStilling> ressursStillinger) {
        return ressursStillinger.stream()
            .filter(rs -> rs.getNavident().isPresent())
            .collect(groupingBy(rs -> rs.getNavident().get()));
    }

    public Map<String, List<StillingPeriode>> strukturerStilingPerioderPerNavident(List<DvhRessursStilling> ressursStillinger) {
        Map<String, List<DvhRessursStilling>> ressursStillingerGruppert = grupperPerNavident(ressursStillinger);
        log.info("Gruppert ressursStilligner på {} navidenter", ressursStillingerGruppert.keySet().size());

        Map<String, List<StillingPeriode>> stillingPerioderPerNavident = new HashMap<>();

        ressursStillingerGruppert.forEach((navident, rsList) -> {
            if (ressursStateRepository.findByNavident(navident).isEmpty()) {
                log.error("Har ressursStillinger fra DVH for navident {} som ikke finnes i NOM", navident);
                return;
            }
            log.debug(SECURE_LOGS, "Strukturer ressursStillinger for navident {} til orgtilknytninger", navident);
            var stillingerGruppert = strukturerNavidentensRessursStillinger(navident, rsList);
            var stillingerMedDagligOppfolging = finnKorrekteDagligOppfolginger(navident, stillingerGruppert);
            if (log.isDebugEnabled()) {
                log.debug(
                    "Resultat av gruppering og matching til orgenhet for navident {}, stillingerGruppert:\n{}, stillingerMedDagligOppfolging:\n{}",
                    navident,
                    StillingPeriode.toString(stillingerGruppert, true),
                    StillingPeriode.toString(stillingerMedDagligOppfolging, true)
                );
            }
            stillingPerioderPerNavident.put(navident, stillingerMedDagligOppfolging);

            log.debug("ressursStillinger for navident {} per orgtilknytninger før: {}, etter: {}", navident, rsList, stillingerMedDagligOppfolging);

        });
        return stillingPerioderPerNavident;
    }

    private List<StillingPeriode> finnKorrekteDagligOppfolginger(String navident, List<StillingPeriode> stillingPerioder) {

        var splittedeStillinger = splitStillingPerioderPaOverlapp(navident, stillingPerioder);
        var sorted = splittedeStillinger.stream().sorted(Comparator.comparing(StillingPeriode::gyldigFom)).toList();

        List<StillingPeriode> medDagligOppfolging = new ArrayList<>();

        sorted.stream().forEachOrdered(stillingPeriode -> {
            var allInThisInterval = sorted.stream()
                .filter(it -> PeriodeUtil.overlapsInterval(stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(), it))
                .toList();
            boolean harNoenLikAgressoIdPaRessursOgStilling = allInThisInterval.stream().anyMatch(StillingPeriode::harSammeAgressoIdPaStillingOgRessurs);
            if (allInThisInterval.size() == 1) {
                // Kun en i perioden, må være DagligOppfolging.JA
                medDagligOppfolging.add(new StillingPeriode(stillingPeriode, DagligOppfolging.JA));
            } else {
                if (PeriodeUtil.isHistoricPeriod(stillingPeriode) || !harNoenLikAgressoIdPaRessursOgStilling) {
                    // Flere i perioden historisk - Kan ikke avgjøre her
                    // Eller at ingen har stilling og ressurs har på samme agressoId
                    // => DagligOppfolging.UKJENT
                    medDagligOppfolging.add(stillingPeriode);
                } else {
                    if (stillingPeriode.harSammeAgressoIdPaStillingOgRessurs()) {
                        // Current eller fremtidig, hvor stilling og ressurs har samme agressoId => DagligOppfolging.Ja
                        medDagligOppfolging.add(new StillingPeriode(stillingPeriode, DagligOppfolging.JA));
                    } else {
                        // Det er en annen som er dagligOppfølging i denne perioden => DagligOppfolging.NEI
                        medDagligOppfolging.add(new StillingPeriode(stillingPeriode, DagligOppfolging.NEI));
                    }
                }
            }
        });
        return medDagligOppfolging;
    }

    // Splitt alle perioder som overlapper slik at overlapp går på samme start slutt
    // Eks:
    // A -> Mai-August
    // B -> Juli-Desemeber
    // Blir:
    // A1 -> Mai-Juni
    // A2 -> Juli-August
    // B1 -> Juli-August
    // B2 -> September-Desember
    private List<StillingPeriode> splitStillingPerioderPaOverlapp(String navident, List<StillingPeriode> stillingPerioder) {
        List<LocalDate> splittFraDatoer =
            stillingPerioder.stream()
                .map(StillingPeriode::gyldigFom)
                .distinct()
                .sorted().toList();
        List<LocalDate> splittTilDatoer =
            stillingPerioder.stream()
                .map(StillingPeriode::gyldigTom)
                .filter(Objects::nonNull)
                .distinct()
                .sorted().toList();

        log.debug(SECURE_LOGS,
            "For navident {} sine stillingPerioder\n{}splitter vi på fra/til-datoene: {} / {}",
            navident,
            StillingPeriode.toString(stillingPerioder, true),
            splittFraDatoer,
            splittTilDatoer
        );
        if (splittFraDatoer.size() <= 1) {
            return stillingPerioder;
        } else {
            List<StillingPeriode> splittedeStillinger = new ArrayList<>();
            stillingPerioder.stream().sorted(Comparator.comparing(StillingPeriode::gyldigFom)).forEachOrdered(stillingPeriode -> {
                LocalDate nesteFraDato = stillingPeriode.gyldigFom();
                LocalDate nesteTilDato = nesteTilDatoEtter(nesteFraDato, stillingPeriode.gyldigTom(), splittFraDatoer, splittTilDatoer);
                while (!Objects.equals(stillingPeriode.gyldigTom(), nesteTilDato)) {
                    splittedeStillinger.add(new StillingPeriode(stillingPeriode.stilling(), nesteFraDato, nesteTilDato, stillingPeriode.orgenhet()));
                    nesteFraDato = nesteTilDato.plusDays(1);
                    nesteTilDato = nesteTilDatoEtter(nesteFraDato, stillingPeriode.gyldigTom(), splittFraDatoer, splittTilDatoer);
                }
                splittedeStillinger.add(new StillingPeriode(stillingPeriode.stilling(), nesteFraDato, nesteTilDato, stillingPeriode.orgenhet()));
            });
            return splittedeStillinger;
        }
    }

    private LocalDate nesteTilDatoEtter(LocalDate nesteFraDato, LocalDate stillingGyldigTom, List<LocalDate> splittFraDatoer, List<LocalDate> splittTilDatoer) {
        int nesteFraDatoIdx = splittFraDatoer.indexOf(nesteFraDato) + 1;
        int nesteTilDatoIdx = splittTilDatoer.indexOf(nesteFraDato) + 1;
        LocalDate nesteSplittFraDato = nesteFraDatoIdx > 0 && nesteFraDatoIdx < splittFraDatoer.size() ? splittFraDatoer.get(nesteFraDatoIdx) : null;
        LocalDate nesteSplittTilDato = splittTilDatoer.stream().filter(tilDato -> tilDato.isAfter(nesteFraDato)).findFirst().orElse(NO_END_DATE);
        return nesteSplittFraDato != null ?
            first(stillingGyldigTom, nesteSplittFraDato.minusDays(1)) :
            first(stillingGyldigTom, nesteSplittTilDato);
    }

    // Null date is consider as latest
    private LocalDate first(LocalDate d1, LocalDate d2) {
        return d1 == null ? d2 : (d2 == null ? d1 : (d1.isBefore(d2) ? d1 : d2));
    }

    public List<StillingPeriode> strukturerNavidentensRessursStillinger(String navident, List<DvhRessursStilling> ressursStillinger) {
        // grupper først på Stillingens - stilling enhet/navn +enhetNavn+ressurs (A+B)
        var grupperteStillinger = ressursStillinger.stream()
            .collect(groupingBy(rs -> new StillingPeriode.StillingAgressoOrgIdOgNavn(rs.getStillingAgressoId(),
                rs.getStillingOrgenhetNavn(),
                rs.getRessursAgressoId(),
                rs.getRessursOrgenhetNiv()
            )));
        List<StillingPeriode> perioderGruppert = new ArrayList<>();

        LocalDate ressursSluttdato = ressursStateRepository.findByNavident(navident).get().sluttdato().orElse(null);

        // Sorter og slå sammen sammenhengende perioder
        grupperteStillinger.forEach((stillingAgressoOrgIdOgNavn, grupperteRessursStillinger) -> {
            log.debug(SECURE_LOGS, "For navident {} - Slår sammen for {}", navident, stillingAgressoOrgIdOgNavn);
            var sorted = grupperteRessursStillinger.stream().sorted(Comparator.comparing(DvhRessursStilling::getStillingDatoFom)).toList();
            if (log.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer("Perioder (");
                sb.append(navident);
                sb.append("):");
                sorted.forEach(rs -> sb.append("\n\t%s - %s".formatted(rs.getStillingDatoFom(), rs.getStillingDatoTom())));
                log.debug(SECURE_LOGS, sb.toString());
            }
            LocalDate periodStart = sorted.get(0).getStillingDatoFom();
            LocalDate previousEnd = sorted.get(0).getStillingDatoTom();
            for (int i = 1; i < sorted.size(); i++) {
                var rs = sorted.get(i);
                var start = rs.getStillingDatoFom();
                var end = rs.getStillingDatoTom();

                if (ressursSluttdato != null && periodStart.isAfter(ressursSluttdato.minusDays(1))) {
                    log.debug(SECURE_LOGS, """
                              DVH ressursStilling for Navident {} starter ({}) på eller etter ressursens sluttdato {}.
                               Ikke behandle flere ressursStillinger for denne navidenten.
                              """, navident, rs.getStillingDatoFom(), ressursSluttdato);

                    break; // Ikke bruk de som starter på eller etter Ressursens sluttdato
                }
                if (ressursSluttdato != null && end.isAfter(ressursSluttdato)) {
                    log.debug(SECURE_LOGS, """
                              DVH ressursStilling for Navident {} har sluttdato ({}) etter ressursens sluttdato {}.
                               Avslutter stillingen på ressursens sluttdato.
                              """, navident, rs.getStillingDatoFom(), ressursSluttdato);
                    end = ressursSluttdato;
                }

                if (start.isEqual(previousEnd.plusDays(1))) {
                    //Stillings raden er  med fortsettelse på forrige stilling - slå sammen
                    previousEnd = end;
                } else if (start.isAfter(previousEnd)) {
                    // Her er det brudd i stilling - Lag en orgtilknytning på forrige periode
                    perioderGruppert.add(
                        new StillingPeriode(stillingAgressoOrgIdOgNavn, periodStart, previousEnd, null)
                    );
                    periodStart = start;
                    previousEnd = end;
                } else {
                    // Her er det overlapp i datoer på samme stillingAgressoId, stillingOrgenhetNavn og stillingRessursAgressoId
                    // Dette håndterer vi ikke per nå
                    log.debug(SECURE_LOGS,
                        "Navident {} har overlapp i stillingsdatoer for '{}' -> forrige periode [{} - {}] og denne stillingen [{} - {}]",
                        navident,
                        stillingAgressoOrgIdOgNavn,
                        periodStart,
                        previousEnd,
                        start,
                        end
                    );
                }
            }
            if (ressursSluttdato == null || periodStart.isBefore(ressursSluttdato)) {
                // Siste RessursStilling er slått sammen
                perioderGruppert.add(
                    new StillingPeriode(stillingAgressoOrgIdOgNavn, periodStart, previousEnd, null)
                );
            }
        });
        if (log.isDebugEnabled()) {
            log.debug(SECURE_LOGS,
                "DVH Periodene for navident {} er nå gruppert:\n{}",
                navident,
                StillingPeriode.toString(
                    perioderGruppert.stream()
                        .sorted(Comparator.comparing(StillingPeriode::gyldigFom))
                        .toList(),
                    true
                )
            );
            log.debug(SECURE_LOGS, "Må nå matche til orgenheter, og periodene evt endres ift til dette");
        }
        // Nå er alle stillingPeriodene gruppert på periode for agressoId.
        // Finn så NOM orgenhet for disse. Hvis gyldig periode for enhet ikke dekker hele perioden for stillingPerioden, så må stillingperioden splittes.
        List<StillingPeriode> perioderGruppertMedOrgenhet = new ArrayList<>();
        perioderGruppert.stream()
            .sorted(Comparator.comparing(StillingPeriode::gyldigFom))
            .forEach(stillingPeriode -> {
                List<IOrgenhet> nomEnheter = Collections.emptyList();
                // Kan kun matche for på stilling agressoId hvis denne finnes og er presis
                if (stillingPeriode.stilling().agressoId().orElse("").length() >= 7) {
                    nomEnheter = finnNomEnheterForIdOgNavn(
                        stillingPeriode.stilling().agressoId().get(),
                        stillingPeriode.stilling().enhetNavn(),
                        stillingPeriode.gyldigFom(),
                        stillingPeriode.gyldigTom()
                    );
                } else if (
                    // Hvis det er en aktiv periode,
                    // og ressurs agressoId er presis
                    // og hvis denne matcher stilling partial eller tom agressoId (Eg. stilling agressoId er overenhet for ressursens agressoId)
                    isCurrentPeriod(stillingPeriode)
                        && stillingPeriode.stilling().ressursAgressoId().orElse("").length() >= 7
                        && stillingPeriode.stilling().ressursAgressoId().get().startsWith(stillingPeriode.stilling().agressoId().orElse(""))
                ) {
                    // så kan vi matche på ressurs sin agressoId
                    nomEnheter = finnNomEnheterForId(
                        stillingPeriode.stilling().ressursAgressoId().get(),
                        stillingPeriode.gyldigFom(),
                        stillingPeriode.gyldigTom()
                    );
                } else {
                    // Kan ikke finne orgenhet for stillingPeriode siden agressoId tilgjengelig ikke er presis (7-8 tegn lang)
                    if (isHistoricPeriod(stillingPeriode)) { // Ikke så farlig
                        log.debug(
                            SECURE_LOGS,
                            "StillingPeriode for navident {} er ikke presis nok på historisk stilling/ressurs sin agressoId til å kunne finne NOM enhet denne tilhører. Den vil ikke behandles:\n  {}",
                            navident,
                            stillingPeriode
                        );
                    } else {
                        String msg = "StillingPeriode for navident %s er ikke presis nok på aktiv/fremtidig stilling/ressurs sin agressoId til å kunne finne NOM enhet denne tilhører. Den vil ikke behandles:\n  %s".formatted(
                            navident,
                            stillingPeriode
                        );
                        ressursStillingLoadStatus.addError(navident, msg);
                        log.warn(msg);
                    }
                }

                if (nomEnheter.size() == 0) {
                    // Klarer ikke finne NOM orgenhet for stillingPeriode/agressoId
                    if (isHistoricPeriod(stillingPeriode)) { // Ikke så farlig
                        log.debug("Klarer ikke finne NOM orgenhet for historisk stillingPeriode/agressoId for navident {}. Den vil ikke behandles:\n\t{}",
                            navident,
                            stillingPeriode
                        );
                    } else {
                        String msg = "Klarer ikke finne NOM orgenhet for aktiv/fremtidig stillingPeriode/agressoId for navident %s. Den vil ikke behandles:\n\t%s".formatted(
                            navident,
                            stillingPeriode
                        );
                        ressursStillingLoadStatus.addError(navident, msg);
                        log.warn(msg);
                    }
                } else if (nomEnheter.size() == 1) {
                    // Exact one match
                    log.debug("StillingPerioden for navident {} matcher en orgenhet: {}", navident, orgenhetToString(nomEnheter.get(0)));
                    // Må nå sjekke om stillingPerioden går utover enhetens periode. Hvis så, må enhetens periode brukes, og overskytende stillingsperiode må forkastes.
                    if (stillingPeriode.gyldigFom().isBefore(nomEnheter.get(0).getGyldigFom())) {
                        log.debug(
                            "Stillingperioden for navident {} starter før kjent orgenhets gyldighetsperiode. Må begrense stillingPeriodens gyldighet til å være innenfor",
                            navident
                        );
                        perioderGruppertMedOrgenhet.add(
                            new StillingPeriode(
                                stillingPeriode.stilling(),
                                nomEnheter.get(0).getGyldigFom(),
                                stillingPeriode.gyldigTom(),
                                nomEnheter.get(0)
                            )
                        );

                        // forsøker på å finne korrekt tidligere enhet for den samme agressoid’en
                        List<IOrgenhet> tidligereNomEnheter = finnNomEnheterForExactId(
                            stillingPeriode.stilling().agressoId().get(),
                            stillingPeriode.gyldigFom(),
                            nomEnheter.get(0).getGyldigFom().minusDays(1)
                        );

                        log.debug(
                            "Klippet før stillingperioden for navident {} fra {} til {}. nomEnheter: {}, stillingPeriode: {}, tidligereNomEnheter: {}",
                            navident,
                            stillingPeriode.gyldigFom(),
                            nomEnheter.get(0).getGyldigFom().minusDays(1),
                            orgenheterToString(nomEnheter),
                            stillingPeriode,
                            orgenheterToString(tidligereNomEnheter)
                        );

                        if(tidligereNomEnheter.isEmpty()){
                            // finner ingen historisk enhet - vi bare logger
                            log.debug(
                                "Finner ingen historisk enhet for stillingperioden for navident {} fra {} til {}. nomEnheter: {}, stillingPeriode: {}",
                                navident,
                                stillingPeriode.gyldigFom(),
                                nomEnheter.get(0).getGyldigFom().minusDays(1),
                                orgenheterToString(nomEnheter),
                                stillingPeriode
                            );
                        }
                        //else if (unleash.isEnabled(UNLEASH_RESSURS_STILLING_STRUKTURER_FIKS_INKONSISTENS)){
                            var historiskStillingsPerioder = new ArrayList<StillingPeriode>();

                            // Fikser inkonsistens i stillingperioden
                             if(tidligereNomEnheter.size() == 1) {
                                 historiskStillingsPerioder.add(
                                     new StillingPeriode(
                                         stillingPeriode.stilling(),
                                         stillingPeriode.gyldigFom(),
                                         nomEnheter.get(0).getGyldigFom().minusDays(1),
                                         tidligereNomEnheter.get(0)
                                     )
                                 );
                             } else {
                                 var previousOrgenhetFom =  nomEnheter.get(0).getGyldigFom().minusDays(1);
                                 var tidligereNomEnheterReserseSortedByTom = reverseSortNomEnheterByGyldigTom(tidligereNomEnheter);

                                 log.debug(
                                     "Sjekker stillingperioden for navident {} fra {} til {}, nomEnheter: {}, tidligereNomEnheter: {} og tidligereNomEnheterReserseSortedByTom: {}",
                                     navident,
                                     stillingPeriode.gyldigFom(),
                                     nomEnheter.get(0).getGyldigFom().minusDays(1),
                                     orgenheterToString(nomEnheter),
                                     orgenheterToString(tidligereNomEnheter),
                                     orgenheterToString(tidligereNomEnheterReserseSortedByTom)
                                 );

                                 for (IOrgenhet oe : tidligereNomEnheterReserseSortedByTom) {
                                     // hvis previousOrgenhetFom er etter oe.getGyldigTom() - log potensielle hull i perioden
                                     if(oe.getGyldigTom().isPresent() && previousOrgenhetFom.isAfter(oe.getGyldigTom().get())){
                                         log.debug("Hull i perioden for navident {} fra {} til {}",
                                             navident,
                                             oe.getGyldigTom().get(),
                                             previousOrgenhetFom);
                                     }

                                     log.debug(
                                         "OE: {}, previousOrgenhetFom - {}",orgenhetToString(oe), previousOrgenhetFom
                                     );

                                     // stillingsperioder
                                     var stillingPeriodeTom = previousOrgenhetFom;
                                     if(oe.getGyldigTom().isPresent()){
                                         stillingPeriodeTom =  oe.getGyldigTom().get().isAfter(previousOrgenhetFom) ? previousOrgenhetFom : oe.getGyldigTom().get(); // if orgEnhets TOM is after previousOrgenhetFom, use previousOrgenhetFom
                                     }
                                     var stillingPeriodeFom = oe.getGyldigFom().isBefore(stillingPeriode.gyldigFom()) ? stillingPeriode.gyldigFom():oe.getGyldigFom(); // if orgEnhets FOM is before stillingFOM, use stillingFOM

                                     historiskStillingsPerioder.add(
                                         new StillingPeriode(
                                             stillingPeriode.stilling(),
                                             stillingPeriodeFom,
                                             stillingPeriodeTom,
                                             oe
                                         ));
                                     previousOrgenhetFom = stillingPeriodeFom.minusDays(1);
                                 }
                             }

                            log.debug(
                                "Fikser inkonsistens i stillingperioden for navident {} fra {} til {}. nomEnheter: {}, stillingPeriode: {}, tidligereNomEnheter: {}, historiskStillingsPerioder: {}",
                                navident,
                                stillingPeriode.gyldigFom(),
                                nomEnheter.get(0).getGyldigFom().minusDays(1),
                                orgenheterToString(nomEnheter),
                                stillingPeriode,
                                orgenheterToString(tidligereNomEnheter),
                                StillingPeriode.toString(historiskStillingsPerioder, true)
                            );
                            if (unleash.isEnabled(UNLEASH_RESSURS_STILLING_STRUKTURER_FIKS_INKONSISTENS)){
                                perioderGruppertMedOrgenhet.addAll(historiskStillingsPerioder);
                            }
                        //}
                    } else if (stillingPeriode.gyldigTom().isAfter(nomEnheter.get(0).getGyldigTom().orElse(NO_END_DATE))) {
                        log.debug(
                            "Stillingperioden for navident {} slutter etter kjent orgenhets gyldighetsperiode. Må begrense stillingPeriodens gyldighet til å være innenfor",
                            navident
                        );
                        perioderGruppertMedOrgenhet.add(
                            new StillingPeriode(
                                stillingPeriode.stilling(),
                                stillingPeriode.gyldigFom(),
                                nomEnheter.get(0).getGyldigTom().orElse(NO_END_DATE),
                                nomEnheter.get(0)
                            )
                        );

                        // forsøker på å finne korrekt seinere enhet for den samme agressoid’en
                        List<IOrgenhet> seinereNomEnherer = Collections.emptyList();
                        if(nomEnheter.get(0).getGyldigTom().isPresent()){
                            seinereNomEnherer =  finnNomEnheterForExactId(
                                stillingPeriode.stilling().agressoId().get(),
                                nomEnheter.get(0).getGyldigTom().get().plusDays(1),
                                stillingPeriode.gyldigTom()
                            );
                        }

                        log.debug(
                            "Klippet etter stillingperioden for navident {} fra {} til {}. nomEnheter: {}, stillingPeriode: {}, seinereNomEnherer: {}",
                            navident,
                            nomEnheter.get(0).getGyldigTom().orElse(NO_END_DATE),
                            stillingPeriode.gyldigTom(),
                            orgenheterToString(nomEnheter),
                            stillingPeriode,
                            orgenheterToString(seinereNomEnherer)
                        );

                        // Fikser inkonsistens i stillingperioden
                        if (!seinereNomEnherer.isEmpty() && unleash.isEnabled(UNLEASH_RESSURS_STILLING_STRUKTURER_FIKS_INKONSISTENS)){
                            // NOTE: kanskje vi må legge til alle hvis seinereNomEnherer.size() > 1. Denne er ikke så realistisk case! Logger warning hvis skjer
                            if(seinereNomEnherer.size() > 1 ) {
                                log.warn(
                                    "Finner flere seinere enheter for stillingperioden for navident {} fra {} til {}. seinereNomEnherer: {}",
                                    navident,
                                    nomEnheter.get(0).getGyldigTom().orElse(NO_END_DATE),
                                    stillingPeriode.gyldigTom(),
                                    orgenheterToString(seinereNomEnherer)
                                );
                            } else {
                                log.debug(
                                    "Fikser inkonsistens i stillingperioden for navident {} fra {} til {}. seinereNomEnherer: {}",
                                    navident,
                                    nomEnheter.get(0).getGyldigTom().orElse(NO_END_DATE),
                                    stillingPeriode.gyldigTom(),
                                    orgenheterToString(seinereNomEnherer)
                                );
                                perioderGruppertMedOrgenhet.add(
                                    new StillingPeriode(
                                        stillingPeriode.stilling(),
                                        nomEnheter.get(0).getGyldigTom().get().plusDays(1),
                                        stillingPeriode.gyldigTom(),
                                        seinereNomEnherer.get(0)
                                    )
                                );
                            }
                        }
                    } else {
                        perioderGruppertMedOrgenhet.add(new StillingPeriode(stillingPeriode, nomEnheter.get(0)));
                    }
                } else {
                    final var nomEnheterString = orgenheterToString(nomEnheter);
                    // Match på flere orgenheter i NOM. Må splitte stillingPerioden...
                    log.debug(SECURE_LOGS, "StillingPerioden {} for navident {} matcher flere NOM Orgenheter\t{}", navident, stillingPeriode, nomEnheterString);
                    List<StillingPeriode> nyeStillingPerioder = new ArrayList<>();
                    AtomicReference<LocalDate> nextGyldigFom = new AtomicReference<>(stillingPeriode.gyldigFom());
                    nomEnheter.stream()
                        .sorted(Comparator.comparing(IOrgenhet::getGyldigFom))
                        .forEach(oe -> {
                            if (nextGyldigFom.get().isAfter(oe.getGyldigTom().orElse(NO_END_DATE).minusDays(1))) {
                                // Har truffet snag med match på flere enheter som har overlapp i periode
                                // Vi må baile ut - Kan ikke matche på flere enheter. Orgenheter må korrigeres
                                String msg = "Har funnet matchende orgenheter\n%s med overlappende perioder for stillingperiode for navident %s \n%s.\nDette kan ikke behandles.".formatted(
                                    navident,
                                    nomEnheterString,
                                    stillingPeriode
                                );
                                ressursStillingLoadStatus.addError(navident, msg);
                                log.error(msg);
                            } else {
                                nyeStillingPerioder.add(new StillingPeriode(
                                    stillingPeriode.stilling(),
                                    nextGyldigFom.get(),
                                    oe.getGyldigTom().orElse(NO_END_DATE),
                                    oe
                                ));
                                nextGyldigFom.set(oe.getGyldigTom().orElse(NO_END_DATE).plusDays(1));
                            }
                        });
                    log.debug(SECURE_LOGS,
                        "For navident {} - Splittet opp i\n\t{}",
                        navident,
                        nyeStillingPerioder.stream().map(StillingPeriode::toString).collect(Collectors.joining("\n\t"))
                    );
                    perioderGruppertMedOrgenhet.addAll(nyeStillingPerioder);
                }
            });


        log.debug(
            "perioderGruppertMedOrgenhet for navident {}, perioder: {}",
            navident,
            perioderGruppertMedOrgenhet.stream().map(StillingPeriode::toString).collect(Collectors.joining("\n"))
        );
        return perioderGruppertMedOrgenhet;
    }

   public List<IOrgenhet>  reverseSortNomEnheterByGyldigTom(List<IOrgenhet> nomEnheter){
       return nomEnheter.stream().sorted(Comparator.comparing(oe -> oe.getGyldigTom().orElse(LocalDate.MAX))).collect(Collectors.toList()).reversed();
   }

    // Alle ORGENHET enheter med matched agressoId som overlapper i perioden
    public List<IOrgenhet> finnNomEnheterForId(String agressoId, LocalDate gyldigFom, LocalDate gyldigTom) {
        return orgenhetService.matchByAgressoId(agressoId).stream()
            .filter(oe ->
                "ORGENHET".equals(oe.getOrgNiv()) &&
                    (gyldigTom.isAfter(oe.getGyldigFom().minusDays(1)) && gyldigFom.isBefore(oe.getGyldigTom().orElse(NO_END_DATE.plusDays(1))))
            )
            .filter(oe -> !oe.getGyldigFom().equals(oe.getGyldigTom().orElse(null))) // Ekskluder enheter hvor fra-til dato er lik
            .toList();
    }

    // Alle ORGENHET enheter med exact agressoId som overlapper i perioden
    public List<IOrgenhet> finnNomEnheterForExactId(String agressoId, LocalDate gyldigFom, LocalDate gyldigTom) {
        return orgenhetService.exactMatchByAgressoId(agressoId).stream()
            .filter(oe ->
                "ORGENHET".equals(oe.getOrgNiv()) &&
                    (gyldigTom.isAfter(oe.getGyldigFom().minusDays(1)) && gyldigFom.isBefore(oe.getGyldigTom().orElse(NO_END_DATE.plusDays(1))))
            )
            .filter(oe -> !oe.getGyldigFom().equals(oe.getGyldigTom().orElse(null))) // Ekskluder enheter hvor fra-til dato er lik
            .toList();
    }


    // Alle ORGENHET enheter med matched agressoId og navn som overlapper i perioden
    public List<IOrgenhet> finnNomEnheterForIdOgNavn(String agressoId, String enhetNavn, LocalDate gyldigFom, LocalDate gyldigTom) {
        return orgenhetService.matchByAgressoId(agressoId).stream()
            .filter(oe ->
                "ORGENHET".equals(oe.getOrgNiv()) &&
                    enhetNavn.equals(oe.getNavn()) &&
                    (gyldigTom.isAfter(oe.getGyldigFom().minusDays(1)) && gyldigFom.isBefore(oe.getGyldigTom().orElse(NO_END_DATE.plusDays(1))))
            )
            .filter(oe -> !oe.getGyldigFom().equals(oe.getGyldigTom().orElse(null))) // Ekskluder enheter hvor fra-til dato er lik
            .toList();
    }

    private String orgenheterToString(List<IOrgenhet> orgenheter) {
        StringBuffer str = new StringBuffer();
        orgenheter.forEach(oe ->
            str.append("\n\t")
                .append(orgenhetToString(oe))
        );
        return str.toString();
    }

    private String orgenhetToString(IOrgenhet orgenhet) {
        StringBuffer str = new StringBuffer();
        str.append(orgenhet.getId())
            .append("-")
            .append(orgenhet.getNavn())
            .append(" [")
            .append(orgenhet.getGyldigFom())
            .append(" - ")
            .append(orgenhet.getGyldigTom().orElse(NO_END_DATE))
            .append("]")
            .append(" - ")
            .append(orgenhet.getUniqueAgressoId());
        return str.toString();
    }

    private boolean isLocalEnvRun() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("local"::equals);
    }
}
