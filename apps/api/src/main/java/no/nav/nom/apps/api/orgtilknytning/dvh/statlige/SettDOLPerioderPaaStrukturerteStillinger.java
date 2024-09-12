package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Comparator.comparing;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.NO_END_DATE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.dayAfter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.dayBefore;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.etter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.foer;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerFoer;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.overlapsInterval;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.tidligst;
import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.Util.getEarliestDate;
import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.Util.slaaSammenBackToBackPerioderOgSorterAsc;

@Component
@Slf4j
public class SettDOLPerioderPaaStrukturerteStillinger {
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;

    public SettDOLPerioderPaaStrukturerteStillinger(OrgtilknytningStateRepository orgtilknytningStateRepository) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
    }

    public void behandle(StrukturerteStillingPerioderForNavident sspForNavident) {
        var fomDatoer = finnBrekkDatoer(sspForNavident); // Sortert
        var splittedeSPer = splittStillingPerioder(sspForNavident.stillingPerioder(), fomDatoer);

        var historicFomDates = historicFomDates(fomDatoer);
        var currentFomDate = currentFomDate(fomDatoer);
        var futureFomDates = futureFomDates(fomDatoer);

        List<StillingPeriode> historicStillingPerioder =  handleHistoricPerioder(sspForNavident, splittedeSPer.stream()
                .filter(sp -> historicFomDates.contains(sp.gyldigFom()))
                .toList(),
            historicFomDates
        );

        List<StillingPeriode> currentStillingPerioder =  handleCurrentPerioder(sspForNavident, splittedeSPer.stream()
            .filter(sp -> sp.gyldigFom().equals(currentFomDate))
            .toList()
        );

        List<StillingPeriode> futureStillingPerioder =  handleFuturePerioder(sspForNavident, splittedeSPer.stream()
            .filter(sp -> futureFomDates.contains(sp.gyldigFom()))
            .toList(),
            futureFomDates
        );

        List<StillingPeriode> nyeStillingPerioder = new ArrayList<>();
        nyeStillingPerioder.addAll(historicStillingPerioder);
        nyeStillingPerioder.addAll(currentStillingPerioder);
        nyeStillingPerioder.addAll(futureStillingPerioder);

        sspForNavident.replaceStillingPerioder(slaaSammenBackToBackPerioderOgSorterAsc(nyeStillingPerioder, sspForNavident));
    }

    // Alle mulige fra og med datoer
    protected List<LocalDate> finnBrekkDatoer(StrukturerteStillingPerioderForNavident sspForNavident) {
        List<LocalDate> brekkDatoer = new ArrayList<>();
        sspForNavident.stillingPerioder().stream()
                .map(StillingPeriode::gyldigFom)
                .forEach(brekkDatoer::add);

        sspForNavident.stillingPerioder().stream()
            .map(StillingPeriode::gyldigTom)
            .map(PeriodeUtil::dayAfter)
            .forEach(brekkDatoer::add);

        orgtilknytningStateRepository.findByNavident(sspForNavident.navident()).stream()
                .map(OrgtilknytningState::gyldigFom)
                .forEach(brekkDatoer::add);

        orgtilknytningStateRepository.findByNavident(sspForNavident.navident()).stream()
            .map( ot -> ot.gyldigTom().orElse(NO_END_DATE))
            .map(PeriodeUtil::dayAfter)
            .forEach(brekkDatoer::add);

        return brekkDatoer.stream().distinct().sorted().toList();
    }

    List<StillingPeriode> splittStillingPerioder(List<StillingPeriode> stillingPerioder, List<LocalDate> brekkDatoer) {
        Map<StillingPeriode, List<LocalDate>> brekkDatoerPerStillingPeriode = new LinkedHashMap<>();
        brekkDatoer.forEach(brekkDato -> {
            stillingPerioder.stream()
                    .filter( sp -> sp.covers(brekkDato))
                    .forEach(sp -> {
                        if (!brekkDatoerPerStillingPeriode.containsKey(sp)) {
                            brekkDatoerPerStillingPeriode.put(sp, new ArrayList<>());
                        }
                        if (!sp.gyldigFom().equals(brekkDato)) {
                            brekkDatoerPerStillingPeriode.get(sp).add(brekkDato);
                        }
                    });
        });

        List<StillingPeriode> splittedeStillingPerioder = new ArrayList<>();

        brekkDatoerPerStillingPeriode.forEach((sp, brekkDatoerForSP) -> {
            LocalDate currentFom = sp.gyldigFom();
            for (LocalDate brekkDato : brekkDatoerForSP) {
                splittedeStillingPerioder.add(new StillingPeriode(sp, currentFom, dayBefore(brekkDato)));
                currentFom = brekkDato;
            }
            splittedeStillingPerioder.add(new StillingPeriode(sp, currentFom, sp.gyldigTom()));
        });
        return splittedeStillingPerioder;
    }

    private List<StillingPeriode> handleHistoricPerioder(StrukturerteStillingPerioderForNavident sspForNavident, List<StillingPeriode> stillingPerioder, List<LocalDate> historicFomDates) {
        List<StillingPeriode> historicStillingPerioder =  new ArrayList<>();
        historicFomDates.forEach( historicFomDate ->
            historicStillingPerioder.addAll(
                handleHistoricPeriode( // Per periode
                    sspForNavident,
                    stillingPerioder.stream()
                        .filter(sp -> sp.gyldigFom().equals(historicFomDate))
                        .toList()
                )
            )
        );
        return historicStillingPerioder;
    }

    private List<StillingPeriode> handleHistoricPeriode(StrukturerteStillingPerioderForNavident sspForNavident, List<StillingPeriode> stillingPerioder) {
        if (stillingPerioder.stream().map(StillingPeriode::gyldigFom).distinct().count() > 1) {
            throw new RuntimeException("Kan ikke behandle ulike historiske perioder samtidig");
        }

        if (stillingPerioder.size() == 1) {
            return List.of(new StillingPeriode(stillingPerioder.getFirst(), StillingPeriode.DagligOppfolging.JA));
        }

        final List<StillingPeriode> nyeHistoriskeStillingPerioder = new ArrayList<>();

        // StillingPeriodene er sortert på gyldigFom
        stillingPerioder.forEach(stillingPeriode ->
            nyeHistoriskeStillingPerioder.add(handleMatchMotEksisterendeOtForHistoriskPeriode(sspForNavident, stillingPeriode))
        );

        if (nyeHistoriskeStillingPerioder.stream().anyMatch(sp -> !sp.erDagligOppfolging())) {
            //Har ikke klart å sette DOL på aktiv periode via eksisterende OTer
            stillingPerioder.forEach(stillingPeriode -> {
                nyeHistoriskeStillingPerioder.add(matchDOLMotOrgenhetPaaRessurs(stillingPeriode));
            });
        }

        //Har vi ikke klart å sette DOL på aktiv periode - Logg og advar
        if (nyeHistoriskeStillingPerioder.stream().anyMatch(sp -> !sp.erDagligOppfolging())) {
            lagErrorHistoricPeriodeUtenDOL(sspForNavident, nyeHistoriskeStillingPerioder);
        }

        // Har vi laget flere med DOL
        if (nyeHistoriskeStillingPerioder.stream().filter(sp -> !sp.erDagligOppfolging()).count() > 1) {
            throw new RuntimeException("Navident %s kan ikke ha flere DOL perioder for samme historiske periode".formatted(sspForNavident.navident()));
        }

        return nyeHistoriskeStillingPerioder;
    }


    private List<StillingPeriode> handleCurrentPerioder(StrukturerteStillingPerioderForNavident sspForNavident, List<StillingPeriode> stillingPerioder) {
        if (stillingPerioder.stream().map(StillingPeriode::gyldigFom).distinct().count() > 1) {
            throw new RuntimeException("Kan ikke behandle ulike current perioder samtidig - Noe gikk feil i SplittStillingPerioder");
        }

        if (stillingPerioder.size() == 1) {
            return List.of(new StillingPeriode(stillingPerioder.getFirst(), StillingPeriode.DagligOppfolging.JA));
        }

        List<StillingPeriode> behandledeStillingPerioder = new ArrayList<>();
        stillingPerioder.forEach(stillingPeriode -> {
            behandledeStillingPerioder.add(matchDOLMotOrgenhetPaaRessurs(stillingPeriode));
        });

        //Har vi ikke klart å sette DOL på aktiv periode
        if (behandledeStillingPerioder.stream().anyMatch(sp -> !sp.erDagligOppfolging())) {
            lagErrorCurrentPeriodeUtenDOL(sspForNavident, behandledeStillingPerioder);
        }

        return behandledeStillingPerioder;
    }

    private List<StillingPeriode> handleFuturePerioder(StrukturerteStillingPerioderForNavident sspForNavident, List<StillingPeriode> stillingPerioder, List<LocalDate> futureFomDates) {
        List<StillingPeriode> futureStillingPerioder =  new ArrayList<>();
        futureFomDates.forEach( historicFomDate ->
            futureStillingPerioder.addAll(
                handleFuturePeriode( // Per periode
                    sspForNavident,
                    stillingPerioder.stream()
                        .filter(sp -> sp.gyldigFom().equals(historicFomDate))
                        .toList()
                )
            )
        );
        return futureStillingPerioder;
    }

    private List<StillingPeriode> handleFuturePeriode(StrukturerteStillingPerioderForNavident sspForNavident, List<StillingPeriode> stillingPerioder) {
        if (stillingPerioder.stream().map(StillingPeriode::gyldigFom).distinct().count() > 1) {
            throw new RuntimeException("Kan ikke behandle ulike fremtidige perioder samtidig");
        }

        if (stillingPerioder.size() == 1) {
            return List.of(new StillingPeriode(stillingPerioder.getFirst(), StillingPeriode.DagligOppfolging.JA));
        }

        final List<StillingPeriode> nyeFremtidigStillingPerioder = new ArrayList<>();

        // StillingPeriodene er sortert på gyldigFom
        stillingPerioder.forEach(stillingPeriode ->
            nyeFremtidigStillingPerioder.add(matchDOLMotOrgenhetPaaRessurs(stillingPeriode))
        );

        //Har vi ikke klart å sette DOL på aktiv periode - Logg og advar
        if (nyeFremtidigStillingPerioder.stream().anyMatch(sp -> !sp.erDagligOppfolging())) {
            lagErrorFuturePeriodeUtenDOL(sspForNavident, nyeFremtidigStillingPerioder);
        }

        // Har vi laget flere med DOL
        if (nyeFremtidigStillingPerioder.stream().filter(sp -> !sp.erDagligOppfolging()).count() > 1) {
            throw new RuntimeException("Navident %s kan ikke ha flere DOL perioder for samme fremtidige periode".formatted(sspForNavident.navident()));
        }

        return nyeFremtidigStillingPerioder;
    }

    private StillingPeriode handleMatchMotEksisterendeOtForHistoriskPeriode(
            StrukturerteStillingPerioderForNavident sspForNavident, StillingPeriode stillingPeriode
    ) {
        // Finn eksisterende OT(er) for denne perioden
        var OTer = orgtilknytningStateRepository.findByNavident(sspForNavident.navident()).stream()
                .filter(ot ->
                        stillingPeriode.orgenhet().getId().equals(ot.orgEnhetId()) &&
                                stillingPeriode.overlaps(ot)
                )
                .sorted(comparing(OrgtilknytningState::gyldigFom))
                .toList();

        if (OTer.isEmpty()) {
            // Hvis ingen OT er funnet - bare returner stillingPeriode uendret
            return stillingPeriode;
        } if (OTer.size() > 1) {
            // Finnes flere eksisterende OTer som overlapper med samme enhet i perioden - dette er inkonsistens i eksisterende OT'er
            // Rapporter med Error - og fortsett med å velge DOl status fra første OT i listen
            lagErrorDuplikateOTerIHistoriskPeriode(sspForNavident, stillingPeriode, OTer);
        }
        return new StillingPeriode(stillingPeriode,
            OTer.getFirst().erDagligOppfolging() ? StillingPeriode.DagligOppfolging.JA : StillingPeriode.DagligOppfolging.NEI);
    }

    private StillingPeriode matchDOLMotOrgenhetPaaRessurs(StillingPeriode stillingPeriode) {
        return new StillingPeriode(stillingPeriode,
            stillingPeriode.orgenhet().getAgressoId().startsWith(stillingPeriode.stilling().ressursAgressoId().orElse("NULL")) ?
                StillingPeriode.DagligOppfolging.JA : StillingPeriode.DagligOppfolging.NEI);
    }

    protected List<LocalDate> historicFomDates(List<LocalDate> fomDatoer) {
        return fomDatoer.stream()
            .filter(d -> foer(d, tidligst(currentFomDate(fomDatoer), LocalDate.now())))
            .toList();
    }

    protected LocalDate currentFomDate(List<LocalDate> fomDatoer) {
        return fomDatoer.stream()
            .filter(d -> likEllerFoer(d, LocalDate.now()))
            .max(LocalDate::compareTo)
            .orElse(null);
    }

    protected List<LocalDate> futureFomDates(List<LocalDate> fomDatoer) {
        return fomDatoer.stream()
            .filter(d -> etter(d, tidligst(currentFomDate(fomDatoer), LocalDate.now())))
            .toList();
    }

    private void lagErrorCurrentPeriodeUtenDOL(
            StrukturerteStillingPerioderForNavident sspForNavident,
            List<StillingPeriode> aktiveStillingPerioder
    ) {
        var fom = aktiveStillingPerioder.getFirst().gyldigFom();
        var tom = aktiveStillingPerioder.getFirst().gyldigTom();
        var orgenhetPaaRessurs = aktiveStillingPerioder.getFirst().stilling().ressursAgressoId().orElse("NULL");

        List<String> enheter = aktiveStillingPerioder.stream()
                .map(sp -> sp.orgenhet().getId() + "-" + sp.orgenhet().getNavn() + " (" + sp.orgenhet().getAgressoId() + ")")
                .toList();

        sspForNavident.addError(
                StrukturerteStillingPerioderForNavident.ErrorType.CURRENT_PERIODE_KAN_IKKE_MATCHE_ORGENHET_PAA_RESSURS_FOR_DOL,
                """
                Aktiv periode [%s - %s] for Navident %s har ingen StillingPeriode som matcher 'orgenhet på ressurs' (%s).
                Har StillingsPerioder for følgende orgenheter %s.
                Dette gjør at vi mangler DOL for aktiv periode.
                """
                        .formatted(fom, tom, sspForNavident.navident(), orgenhetPaaRessurs, enheter)
        );
    }

    private void lagErrorHistoricPeriodeUtenDOL(StrukturerteStillingPerioderForNavident sspForNavident, List<StillingPeriode> historiskeStillingPerioder) {
        var fom = historiskeStillingPerioder.getFirst().gyldigFom();
        var tom = historiskeStillingPerioder.getFirst().gyldigTom();

        List<String> enheter = historiskeStillingPerioder.stream()
            .map(sp -> sp.orgenhet().getId() + "-" + sp.orgenhet().getNavn() + " (" + sp.orgenhet().getAgressoId() + ")")
            .toList();

        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.HISTORIC_PERIODE_KAN_IKKE_SETTE_DOL,
            """
            Klarer ikke sette DOL for historisk periode [%s - %s] for Navident %s.
            Har StillingsPerioder for følgende orgenheter %s.
            """
                .formatted(fom, tom, sspForNavident.navident(), enheter)
        );
    }


    private void lagErrorFuturePeriodeUtenDOL(StrukturerteStillingPerioderForNavident sspForNavident, List<StillingPeriode> fremtidigStillingPerioder) {
        var fom = fremtidigStillingPerioder.getFirst().gyldigFom();
        var tom = fremtidigStillingPerioder.getFirst().gyldigTom();

        List<String> enheter = fremtidigStillingPerioder.stream()
            .map(sp -> sp.orgenhet().getId() + "-" + sp.orgenhet().getNavn() + " (" + sp.orgenhet().getAgressoId() + ")")
            .toList();

        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.FUTURE_PERIODE_KAN_IKKE_SETTE_DOL,
            """
            Klarer ikke sette DOL for fremtidig periode [%s - %s] for Navident %s.
            Har StillingsPerioder for følgende orgenheter %s.
            """
                .formatted(fom, tom, sspForNavident.navident(), enheter)
        );
    }

    private void lagErrorDuplikateOTerIHistoriskPeriode(
        StrukturerteStillingPerioderForNavident sspForNavident, StillingPeriode stillingPeriode, List<OrgtilknytningState> oTer
    ) {
        var fom = stillingPeriode.gyldigFom();
        var tom = stillingPeriode.gyldigTom();

        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.DUPLIKATE_EKSISTERENDE_ORGTILKNYNINGER_I_HISTORISK_PERIODE,
            """
            I historisk periode [%s - %s] for Navident %s finnes det flere (%s) eksisterende OTer som overlapper med samme enhet.
            Dette gjelder Orgtilknytningene med fid %s.
            """
                .formatted(fom, tom, sspForNavident.navident(), oTer.size(), oTer.stream().map(OrgtilknytningState::fid).toList())
        );
    }
}
