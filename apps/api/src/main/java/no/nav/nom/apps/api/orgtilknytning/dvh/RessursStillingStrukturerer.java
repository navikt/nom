package no.nav.nom.apps.api.orgtilknytning.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.IOrgenhetService;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.NO_END_DATE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.isCurrentPeriode;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.isHistoricPeriod;

@Component
@Slf4j
public class RessursStillingStrukturerer {
    private final IOrgenhetService orgenhetService;
    private final Environment environment;
    private static int NUM_TO_PROCESS_PER_NAVIDENT_LETTER = -1; // mindre enn 1 tar med alle - Brukes kun for testing under lokal utvikling

    public RessursStillingStrukturerer(IOrgenhetService orgenhetService, Environment environment) {
        this.orgenhetService = orgenhetService;
        this.environment = environment;
    }

    public Map<String, List<DvhRessursStilling>> grupperPerNavident(List<DvhRessursStilling> ressursStillinger) {
        return ressursStillinger.stream()
            .filter(rs -> rs.getNavident().isPresent())
            .collect(groupingBy(rs -> rs.getNavident().get()));
    }

    public Map<String, List<StillingPeriode>> StrukturerStilingPerioderPerNavident(List<DvhRessursStilling> ressursStillinger) {
        Map <String, List<DvhRessursStilling>> ressursStillingerGruppert = grupperPerNavident(ressursStillinger);
        log.debug("Gruppert ressursStilligner på {} navidenter", ressursStillingerGruppert.keySet().size());

        Map <String, List<StillingPeriode>> stillingPerioderPerNavident = new HashMap<>();

        ressursStillingerGruppert.forEach((navident, rsList) -> {

            if (harBehandletNok_LOCAL_RUN(stillingPerioderPerNavident, navident)) return; // Har nå behandlet nok navidenter som starter på denne bokstaven. Hop til neste

            log.debug("Strukturer ressursStillinger for navident {} til orgtilknytninger", navident);
            var stillingerGruppert = strukturerNavidentensRessursStillinger(navident, rsList);
            if (log.isDebugEnabled()) {
                log.debug("Resultat av gruppering og matching til orgenhet:\n{}", StillingPeriode.stillingperioderToString(stillingerGruppert, true));
                stillingPerioderPerNavident.put(navident, stillingerGruppert);
            }
        });
        return stillingPerioderPerNavident;
    }

    public List<StillingPeriode> strukturerNavidentensRessursStillinger(String navident, List<DvhRessursStilling> ressursStillinger) {
        // grupper først på Stillingens - stilling enhet/navn +enhetNavn+ressurs (A+B)
        var grupperteStillinger = ressursStillinger.stream().collect(groupingBy( rs -> new StillingPeriode.StillingAgressoOrgIdOgNavn(rs.getStillingAgressoId(), rs.getStillingOrgenhetNavn(), rs.getRessursAgressoId(), rs.getRessursOrgenhetNiv())));
        List<StillingPeriode> perioderGruppert = new ArrayList<>();

        // Sorter og slå sammen sammenhengende perioder
        grupperteStillinger.forEach( (stillingAgressoOrgIdOgNavn, grupperteRessursStillinger)  -> {
            log.debug("Slår sammen for {}", stillingAgressoOrgIdOgNavn);
            var sorted = grupperteRessursStillinger.stream().sorted(Comparator.comparing(DvhRessursStilling::getStillingDatoFom)).toList();
            if (log.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer("Perioder:");
                sorted.forEach( rs -> sb.append("\n\t%s - %s".formatted(rs.getStillingDatoFom(), rs.getStillingDatoTom())));
                log.debug(sb.toString());
            }
            LocalDate periodStart = sorted.get(0).getStillingDatoFom();
            LocalDate previousEnd = sorted.get(0).getStillingDatoTom();
            for (int i = 1; i < sorted.size(); i++) {
                var rs = sorted.get(i);
                var start = rs.getStillingDatoFom();
                var end =  rs.getStillingDatoTom();
                if(start.isEqual(previousEnd.plusDays(1))) {
                    //Stillings raden er  med fortsettelse på forrige stilling - slå sammen
                    previousEnd = end;
                } else if (start.isAfter(previousEnd)){
                    // Her er det brudd i stilling - Lag en orgtilknytning på forrige periode
                    perioderGruppert.add(
                        new StillingPeriode(stillingAgressoOrgIdOgNavn, periodStart, previousEnd, null)
                    );
                    periodStart = start;
                    previousEnd = end;
                } else {
                    // Her er det overlapp i datoer på samme stillingAgressoId, stillingOrgenhetNavn og stillingRessursAgressoId
                    // Dette håndterer vi ikke per nå
                    log.debug("NAvident {} har overlapp i stillingsdatoer for '{}' -> forrige periode [{} - {}] og denne stillingen [{} - {}]", navident, stillingAgressoOrgIdOgNavn, periodStart, previousEnd, start, end);
                }
            }
            // Siste RessursStilling er slått sammen
            perioderGruppert.add(
                new StillingPeriode(stillingAgressoOrgIdOgNavn, periodStart, previousEnd, null)
            );
        });
        if (log.isDebugEnabled()) {
            log.debug("DVH Periodene er nå gruppert:\n{}",
                StillingPeriode.stillingperioderToString(
                    perioderGruppert.stream()
                        .sorted(Comparator.comparing(StillingPeriode::gyldigFom))
                        .toList(),
                    true));
            log.debug("Må nå matche til orgenheter, og periodene evt endres ift til dette");
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
                    nomEnheter = finnNomEnheterForIdOgNavn(stillingPeriode.stilling().agressoId().get(), stillingPeriode.stilling().enhetNavn(), stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom());
                } else if (
                    // Hvis det er en aktiv periode,
                    // og ressurs agressoId er presis
                    // og hvis denne matcher stilling partial eller tom agressoId (Eg. stilling agressoId er overenhet for ressursens agressoId)
                        isCurrentPeriode(stillingPeriode)
                        && stillingPeriode.stilling().ressursAgressoId().orElse("").length() >= 7
                        && stillingPeriode.stilling().ressursAgressoId().get().startsWith(stillingPeriode.stilling().agressoId().orElse(""))
                ) {
                    // så kan vi matche på ressurs sin agressoId
                    nomEnheter = finnNomEnheterForId(stillingPeriode.stilling().ressursAgressoId().get(), stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom());
                } else {
                    // Kan ikke finne orgenhet for stillingPeriode siden agressoId tilgjengelig ikke er presis (7-8 tegn lang)
                    if (isHistoricPeriod(stillingPeriode)) { // Ikke så farlig
                        log.debug("StillingPeriode for navident {} er ikke presis nok på historisk stilling/ressurs sin agressoId til å kunne finne NOM enhet denne tilhører. Den vil ikke behandles:\n  {}", navident, stillingPeriode);
                    } else {
                        log.warn("StillingPeriode for navident {} er ikke presis nok på aktiv/fremtidig stilling/ressurs sin agressoId til å kunne finne NOM enhet denne tilhører. Den vil ikke behandles:\n  {}", navident, stillingPeriode);
                    }
                }

                if (nomEnheter.size() == 0) {
                    // Klarer ikke finne NOM orgenhet for stillingPeriode/agressoId
                    if (isHistoricPeriod(stillingPeriode)) { // Ikke så farlig
                        log.debug("Klarer ikke finne NOM orgenhet for historisk stillingPeriode/agressoId for navident {}. Den vil ikke behandles:\n\t{}", navident, stillingPeriode);
                    } else {
                        log.warn("Klarer ikke finne NOM orgenhet for aktiv/fremtidig stillingPeriode/agressoId for navident {}. Den vil ikke behandles:\n\t{}", navident, stillingPeriode);
                    }
                } else if (nomEnheter.size() == 1) {
                    // Exact one match
                    log.debug("StillingPerioden matcher en orgenhet: {}", orgenhetToString(nomEnheter.get(0)));
                    // Må nå sjekke om stillingPerioden går utover enhetens periode. Hvis så, må enhetens periode brukes, og overskytende stillingsperiode må forkastes.
                    if (stillingPeriode.gyldigFom().isBefore(nomEnheter.get(0).getGyldigFom())) {
                        log.debug("Stillingperioden starter før kjent orgenhets gyldighetsperiode. Må begrense stillingPeriodens gyldighet til å være innenfor");
                        perioderGruppertMedOrgenhet.add(
                            new StillingPeriode(
                                stillingPeriode.stilling(),
                                nomEnheter.get(0).getGyldigFom(),
                                stillingPeriode.gyldigTom(),
                                nomEnheter.get(0))
                        );
                    } else if (stillingPeriode.gyldigTom().isAfter(nomEnheter.get(0).getGyldigTom().orElse(NO_END_DATE))) {
                        log.debug("Stillingperioden slutter etter kjent orgenhets gyldighetsperiode. Må begrense stillingPeriodens gyldighet til å være innenfor");
                        perioderGruppertMedOrgenhet.add(
                            new StillingPeriode(
                                stillingPeriode.stilling(),
                                stillingPeriode.gyldigFom(),
                                nomEnheter.get(0).getGyldigTom().orElse(NO_END_DATE),
                                nomEnheter.get(0))
                        );
                    }else {
                        perioderGruppertMedOrgenhet.add(new StillingPeriode(stillingPeriode, nomEnheter.get(0)));
                    }
                } else {
                    final var nomEnheterString = orgenheterToString(nomEnheter);
                    // Match på flere orgenheter i NOM. Må splitte stillingPerioden...
                    log.debug("StillingPerioden {} matcher flere NOM Orgenheter\t{}", stillingPeriode, nomEnheterString);
                    List<StillingPeriode> nyeStillingPerioder = new ArrayList<>();
                    AtomicReference<LocalDate> nextGyldigFom = new AtomicReference<>(stillingPeriode.gyldigFom());
                    nomEnheter.stream()
                        .sorted(Comparator.comparing(IOrgenhet::getGyldigFom))
                        .forEach( oe -> {
                            if (nextGyldigFom.get().isAfter(oe.getGyldigTom().orElse(NO_END_DATE).minusDays(1))) {
                                // Har truffet snag med match på flere enheter som har overlapp i periode
                                // Vi må baile ut - Kan ikke matche på flere enheter. Orgenheter må korrigeres
                                log.error("Har funnet matchende orgenheter\n{} med overlappende perioder for stillingperiode for navident {} \n{}.\nDette kan ikke behandles.", navident, nomEnheterString, stillingPeriode);
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
                    log.debug("Splittet opp i\n\t{}", nyeStillingPerioder.stream().map(StillingPeriode::toString).collect(Collectors.joining("\n\t")));
                    perioderGruppertMedOrgenhet.addAll(nyeStillingPerioder);
                }
            });

        return perioderGruppertMedOrgenhet;
    }


    // Alle ORGENHET enheter med agressoId som overlapper i perioden
    public List<IOrgenhet> finnNomEnheterForId(String agressoId, LocalDate gyldigFom, LocalDate gyldigTom) {
        return orgenhetService.matchByAgressoId(agressoId).stream()
            .filter( oe ->
                "ORGENHET".equals(oe.getOrgNiv()) &&
                    (gyldigTom.isAfter(oe.getGyldigFom().minusDays(1)) && gyldigFom.isBefore(oe.getGyldigTom().orElse(NO_END_DATE.plusDays(1))))
            )
            .filter( oe -> !gyldigFom.equals(gyldigTom)) // Ekskluder enheter hvor fra-til dato er lik
            .toList();
    }


    // Alle ORGENHET enheter med agressoId og navn som overlapper i perioden
    public List<IOrgenhet> finnNomEnheterForIdOgNavn(String agressoId, String enhetNavn, LocalDate gyldigFom, LocalDate gyldigTom) {
        return orgenhetService.matchByAgressoId(agressoId).stream()
            .filter( oe ->
                "ORGENHET".equals(oe.getOrgNiv()) &&
                    enhetNavn.equals(oe.getNavn()) &&
                    (gyldigTom.isAfter(oe.getGyldigFom().minusDays(1)) && gyldigFom.isBefore(oe.getGyldigTom().orElse(NO_END_DATE.plusDays(1))))
            )
            .filter( oe -> !gyldigFom.equals(gyldigTom)) // Ekskluder enheter hvor fra-til dato er lik
            .toList();
    }

    private boolean harBehandletNok_LOCAL_RUN(Map<String, List<StillingPeriode>> stillingPerioderPerNavident, String navident) {
        return isLocalEnvRun() &&
            NUM_TO_PROCESS_PER_NAVIDENT_LETTER > 0 &&
            stillingPerioderPerNavident.keySet().stream().filter(k -> k.charAt(0) == navident.charAt(0)).count() >= NUM_TO_PROCESS_PER_NAVIDENT_LETTER;
    }

    private String orgenheterToString(List<IOrgenhet> orgenheter) {
        StringBuffer str = new StringBuffer();
        orgenheter.forEach( oe ->
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
           .append("]");
        return str.toString();
    }

    private boolean isLocalEnvRun() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("local"::equals);
    }
}
