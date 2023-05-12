package no.nav.nom.apps.api.orgtilknytning.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.IOrgenhetService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.groupingBy;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.*;

@Component
@Slf4j
public class RessursStillingStrukturerer {
    private final IOrgenhetService orgenhetService;

    private static final LocalDate NO_END_DATE = LocalDate.of(2099, 12, 31);

    public RessursStillingStrukturerer(IOrgenhetService orgenhetService) {
        this.orgenhetService = orgenhetService;
    }

    public Map<String, List<DvhRessursStilling>> grupperPerNavident(List<DvhRessursStilling> ressursStillinger) {
        return ressursStillinger.stream()
            .filter(rs -> rs.getNavident().isPresent())
            .collect(groupingBy(rs -> rs.getNavident().get()));
    }

    public List<StillingPeriode> strukturerNavidentensRessursStillinger(List<DvhRessursStilling> ressursStillinger) {
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
                    log.warn("Overlapp i stillingsdatoer for '{}' -> forrige periode [{} - {}] og denne stillingen [{} - {}]", stillingAgressoOrgIdOgNavn, periodStart, previousEnd, start, end);
                }
            }
            // Siste RessursStilling er slått sammen
            perioderGruppert.add(
                new StillingPeriode(stillingAgressoOrgIdOgNavn, periodStart, previousEnd, null)
            );
        });
        // Nå er alle stillingPeriodene gruppert på periode for agressoId.
        // Finn så NOM orgenhet for disse. Hvis gyldig periode for enhet ikke dekker hele perioden for stillingPerioden, så må stillingperioden splittes.
        List<StillingPeriode> perioderGruppertMedOrgenhet = new ArrayList<>();
        perioderGruppert.stream()
            .sorted(Comparator.comparing(StillingPeriode::gyldigFom))
            .forEach(stillingPeriode -> {
                List<IOrgenhet> nomEnheter = Collections.emptyList();
                // Kan kun matche for på stilling agressoId hvis denne finnes og er presis
                if (stillingPeriode.stilling().agressoId().isPresent() && stillingPeriode.stilling().agressoId().get().length() == 7) {
                    nomEnheter = finnNomEnheterForIdOgNavn(stillingPeriode.stilling().agressoId().get(), stillingPeriode.stilling().enhetNavn(), stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom());
                } else if (
                    // Hvis det er en aktiv periode, og ressurs agressoId er presis
                    // så kan vi matche på ressurs agressoId, hvis denne matcher stilling eller stilling er tom
                    (isCurrentPeriode(stillingPeriode) && stillingPeriode.stilling().ressursAgressoId().isPresent() && stillingPeriode.stilling().ressursAgressoId().get().length() == 7)
                        &&
                        (stillingPeriode.stilling().agressoId().isEmpty() || stillingPeriode.stilling().ressursAgressoId().get().startsWith(stillingPeriode.stilling().agressoId().get()))) {

                    nomEnheter = finnNomEnheterForId(stillingPeriode.stilling().ressursAgressoId().get(), stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom());
                } else {
                    // Kan ikke finne orgenhet for stillingPeriode siden agressoId tilgjengelig ikke er presis (7 lang)
                    log.warn("StillingPeriode er ikke presis nok på stilling/ressurs sin agressoId til å kunne finne NOM enhet denne tilhører. Den vil ikke behandles: {}", stillingPeriode);
                }

                if (nomEnheter.size() == 0) {
                    // Klarer ikke finne NOM orgenhet for stillingPeriode/agressoId
                    log.warn("Klarer ikke finne NOM orgenhet for stillingPeriode/agressoId. Den vil ikke behandles: {}", stillingPeriode);
                } else if (nomEnheter.size() == 1) {
                    // Exact one match
                    perioderGruppertMedOrgenhet.add(new StillingPeriode(stillingPeriode, nomEnheter.get(0)));
                } else {
                    // Match på flere orgenheter i NOM. Må splitte stillingPerioden...
                    log.debug("StillingPerioden {} matcher flere NOM Orgenheter\n  {}", stillingPeriode, orgenheterToString(nomEnheter));
                    List<StillingPeriode> nyeStillingPerioder = new ArrayList<>();
                    AtomicReference<LocalDate> nextGyldigFom = new AtomicReference<>(stillingPeriode.gyldigFom());
                    nomEnheter.stream()
                        .sorted(Comparator.comparing(IOrgenhet::getGyldigFom))
                        .forEach( oe -> {
                            nyeStillingPerioder.add(new StillingPeriode(stillingPeriode.stilling(), nextGyldigFom.get(), oe.getGyldigTom().orElse(NO_END_DATE), oe));
                            nextGyldigFom.set(oe.getGyldigTom().orElse(NO_END_DATE.plusDays(1)));
                        });
                    log.debug("Splittet opp i {}", nyeStillingPerioder);
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
            ).toList();
    }


    // Alle ORGENHET enheter med agressoId og navn som overlapper i perioden
    public List<IOrgenhet> finnNomEnheterForIdOgNavn(String agressoId, String enhetNavn, LocalDate gyldigFom, LocalDate gyldigTom) {
        return orgenhetService.matchByAgressoId(agressoId).stream()
            .filter( oe ->
                "ORGENHET".equals(oe.getOrgNiv()) &&
                    enhetNavn.equals(oe.getNavn()) &&
                    (gyldigTom.isAfter(oe.getGyldigFom().minusDays(1)) && gyldigFom.isBefore(oe.getGyldigTom().orElse(NO_END_DATE.plusDays(1))))
            ).toList();
    }

    public Map<String, List<StillingPeriode>> StrukturerStilingPerioderPerNavident(List<DvhRessursStilling> ressursStillinger) {
        Map <String, List<DvhRessursStilling>> ressursStillingerGruppert = grupperPerNavident(ressursStillinger);
        log.debug("Gruppert ressursStilligner på {} navidenter", ressursStillingerGruppert.keySet().size());

        Map <String, List<StillingPeriode>> stilingPerioderPerNavident = new HashMap<>();

        ressursStillingerGruppert.forEach((navident, rsList) -> {
            log.debug("Strukturer ressursStillinger for navident {} til orgtilknytninger", navident);
            var stillingerGruppert = strukturerNavidentensRessursStillinger(rsList);
            if (log.isDebugEnabled()) {
                log.debug("Resultat:\n{}", StillingPeriode.stillingperioderToString(stillingerGruppert, true));
                stilingPerioderPerNavident.put(navident, stillingerGruppert);
            }
        });
        return stilingPerioderPerNavident;
    }

    private String orgenheterToString(List<IOrgenhet> orgenheter) {
        StringBuffer str = new StringBuffer();
        orgenheter.forEach( oe ->
            str.append("  ")
                .append(oe.getId())
                .append("-")
                .append(oe.getNavn())
                .append(" [")
                .append(oe.getGyldigFom())
                .append(" - ")
                .append(oe.getGyldigTom().orElse(NO_END_DATE))
                .append("]")
        );
        return str.toString();
    }
}
