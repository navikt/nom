package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.NO_END_DATE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.dayBefore;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerEtter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerFoer;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.tidligst;
import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.Util.slaaSammenBackToBackPerioderOgSorterAsc;

@Component
@Slf4j
public class SettDOLPerioderPaaStrukturerteStillinger {
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;

    public SettDOLPerioderPaaStrukturerteStillinger(OrgtilknytningStateRepository orgtilknytningStateRepository) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
    }

    public StrukturerteStillingPerioderForNavident behandle(StrukturerteStillingPerioderForNavident sspForNavident) {
        var fomDatoer = finnBrekkDatoer(sspForNavident); // Sortert
        // TODO: Vi trenger ikke brekk-datoer på OT'er for current og fremtidig?
        var splittedeSPer = splittStillingPerioder(sspForNavident.stillingPerioder(), fomDatoer);


        var historicFomDates = historicFomDates(fomDatoer, splittedeSPer);
        var currentFomDate = currentFomDate(fomDatoer);
        var futureFomDates = futureFomDates(fomDatoer, splittedeSPer);

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

        return new StrukturerteStillingPerioderForNavident(sspForNavident, slaaSammenBackToBackPerioderOgSorterAsc(nyeStillingPerioder, sspForNavident));
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
        brekkDatoer.forEach(brekkDato ->
            stillingPerioder.stream()
                .filter( sp -> sp.covers(brekkDato))
                .forEach(sp -> {
                    if (!brekkDatoerPerStillingPeriode.containsKey(sp)) {
                        brekkDatoerPerStillingPeriode.put(sp, new ArrayList<>());
                    }
                    if (!sp.gyldigFom().equals(brekkDato)) {
                        brekkDatoerPerStillingPeriode.get(sp).add(brekkDato);
                    }
                }));

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
        if (stillingPerioder.isEmpty()) {
            return stillingPerioder;
        }

        List<StillingPeriode> historicStillingPerioder =  new ArrayList<>();
        historicFomDates.forEach( historicFomDate ->
            historicStillingPerioder.addAll(
                handleSingleHistoricPeriode( // Per periode (e.g. alle med samme gyldigFom)
                    sspForNavident,
                    stillingPerioder.stream()
                        .filter(sp -> sp.gyldigFom().equals(historicFomDate))
                        .toList(),
                    historicFomDate
                )
            )
        );
        return historicStillingPerioder;
    }

    private List<StillingPeriode> handleSingleHistoricPeriode(StrukturerteStillingPerioderForNavident sspForNavident, List<StillingPeriode> stillingPerioder, LocalDate historicFomDate) {
        if (stillingPerioder.isEmpty()) {
            lagErrorPeriodeUtenStillingPerioder(sspForNavident, historicFomDate);
            return stillingPerioder;
        }

        if (duplikateStillingPerioderPaaEnhet(stillingPerioder).size() > 1) {
            throw new RuntimeException("Kan ikke behandle duplikate historiske StillingPerioder på samme enhet");
        }

        if (stillingPerioder.size() == 1) {
            return List.of(new StillingPeriode(stillingPerioder.getFirst(), StillingPeriode.DagligOppfolging.JA));
        }

        final List<StillingPeriode> nyeHistoriskeStillingPerioder = new ArrayList<>();

        stillingPerioder.forEach(stillingPeriode ->
            nyeHistoriskeStillingPerioder.add(handleMatchMotEksisterendeOtForHistoriskPeriode(sspForNavident, stillingPeriode))
        );

        if (nyeHistoriskeStillingPerioder.stream().filter(StillingPeriode::erDagligOppfolging).count() > 1) {
            // Har vi laget flere med DOL - E.g. det finnes flere eksisterende OTer som overlapper perioden med DOL
            // Logg og advar - og resolve på OrgEnhet På Ressurs i stedet
            lagErrorHistoricPeriodeMedFlereOTerSomErDOL(sspForNavident, stillingPerioder);
            nyeHistoriskeStillingPerioder.clear();
        }

        if (nyeHistoriskeStillingPerioder.stream().noneMatch(StillingPeriode::erDagligOppfolging)) {
            //Har ikke klart å sette DOL på aktiv periode via eksisterende OTer
            nyeHistoriskeStillingPerioder.clear();
            stillingPerioder.forEach(stillingPeriode -> nyeHistoriskeStillingPerioder.add(matchDOLMotOrgenhetPaaRessurs(stillingPeriode)));
        }

        //Har vi ikke klart å sette DOL på periode - Logg og advar
        if (nyeHistoriskeStillingPerioder.stream().noneMatch(StillingPeriode::erDagligOppfolging)) {
            lagErrorHistoricPeriodeUtenDOL(sspForNavident, stillingPerioder);
        }

        // Har vi laget flere med DOL?
        if (nyeHistoriskeStillingPerioder.stream().filter(StillingPeriode::erDagligOppfolging).count() > 1) {
            log.error("ErrorHistoricPeriodeUtenDOL {} for historicFomDate {} - Stillingperioder: {}", sspForNavident.navident(), historicFomDate, stillingPerioder.stream().map(StillingPeriode::toString).toList());
            log.error("ErrorHistoricPeriodeUtenDOL {} for historicFomDate {} - nyeHistoriskeStillingPerioder: {}", sspForNavident.navident(), historicFomDate, nyeHistoriskeStillingPerioder.stream().map(StillingPeriode::toString).toList());
            log.error("ErrorHistoricPeriodeUtenDOL {} - alle sp'er {}", sspForNavident.navident(), sspForNavident.stillingPerioder().stream().map(StillingPeriode::toString).toList());
            log.error("ErrorHistoricPeriodeUtenDOL {} - alle ot'er {}", sspForNavident.navident(), sspForNavident.eksisterendeOrgtilknytninger().stream().map(OrgtilknytningState::toString).toList());
            throw new RuntimeException("Navident %s kan ikke ha flere DOL perioder for samme historiske periode".formatted(sspForNavident.navident()));
        }

        return nyeHistoriskeStillingPerioder;
    }

    private static Map<IOrgenhet, Long> duplikateStillingPerioderPaaEnhet(List<StillingPeriode> stillingPerioder) {
        return stillingPerioder.stream()
            .collect(collectingAndThen(
                groupingBy(StillingPeriode::orgenhet, counting()),
                m -> {
                    m.values().removeIf(v -> v <= 1L);
                    return m;
                }
            ));
    }

    private List<StillingPeriode> handleCurrentPerioder(StrukturerteStillingPerioderForNavident sspForNavident, List<StillingPeriode> stillingPerioder) {
        if (stillingPerioder.isEmpty()) {
            return stillingPerioder;
        }
        if (stillingPerioder.stream().map(StillingPeriode::gyldigFom).distinct().count() > 1) {
            throw new RuntimeException("Kan ikke behandle ulike current perioder samtidig - Noe gikk feil i SplittStillingPerioder");
        }

        if (stillingPerioder.size() == 1) {
            return List.of(new StillingPeriode(stillingPerioder.getFirst(), StillingPeriode.DagligOppfolging.JA));
        }

        List<StillingPeriode> behandledeStillingPerioder = new ArrayList<>();
        stillingPerioder.forEach(stillingPeriode -> behandledeStillingPerioder.add(matchDOLMotOrgenhetPaaRessurs(stillingPeriode)));

        //Har vi ikke klart å sette DOL på aktiv periode
        if (behandledeStillingPerioder.stream().noneMatch(StillingPeriode::erDagligOppfolging)) {
            lagErrorCurrentPeriodeUtenDOL(sspForNavident, behandledeStillingPerioder);
        }

        return behandledeStillingPerioder;
    }

    private List<StillingPeriode> handleFuturePerioder(StrukturerteStillingPerioderForNavident sspForNavident, List<StillingPeriode> stillingPerioder, List<LocalDate> futureFomDates) {
        if (stillingPerioder.isEmpty()) {
            return stillingPerioder;
        }
        List<StillingPeriode> futureStillingPerioder =  new ArrayList<>();
        futureFomDates.forEach( historicFomDate ->
            futureStillingPerioder.addAll(
                handleFuturePeriode( // Per periode
                    sspForNavident,
                    stillingPerioder.stream()
                        .filter(sp -> sp.gyldigFom().equals(historicFomDate))
                        .toList(),
                    historicFomDate
                )
            )
        );
        return futureStillingPerioder;
    }

    private List<StillingPeriode> handleFuturePeriode(StrukturerteStillingPerioderForNavident sspForNavident, List<StillingPeriode> stillingPerioder, LocalDate futureFomDate) {
        if (stillingPerioder.isEmpty()) {
            lagErrorPeriodeUtenStillingPerioder(sspForNavident, futureFomDate);
            return stillingPerioder;
        }

        if (duplikateStillingPerioderPaaEnhet(stillingPerioder).size() > 1) {
            throw new RuntimeException("Kan ikke behandle duplikate fremtidige StillingPerioder på samme enhet");
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
        if (nyeFremtidigStillingPerioder.stream().noneMatch(StillingPeriode::erDagligOppfolging)) {
            lagErrorFuturePeriodeUtenDOL(sspForNavident, nyeFremtidigStillingPerioder);
        }

        // Har vi laget flere med DOL
        if (nyeFremtidigStillingPerioder.stream().filter(StillingPeriode::erDagligOppfolging).count() > 1) {
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
            // Hvis ingen OT er funnet - bare returner stillingPeriode med DagligOppfolging.NEI
            return new StillingPeriode(stillingPeriode, StillingPeriode.DagligOppfolging.NEI);
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

    protected List<LocalDate> historicFomDates(List<LocalDate> fomDatoer, List<StillingPeriode> stillingPerioder) {
        LocalDate minHistoricFomDate = stillingPerioder.stream().map(StillingPeriode::gyldigFom).min(LocalDate::compareTo).orElse(null);
        LocalDate maxHistoricFomDate = stillingPerioder.stream().map(StillingPeriode::gyldigFom).filter(d -> d.isBefore(LocalDate.now())).max(LocalDate::compareTo).orElse(null);
        return historicFomDates(fomDatoer, minHistoricFomDate, maxHistoricFomDate);
    }

    protected List<LocalDate> historicFomDates(List<LocalDate> fomDatoer, LocalDate minHistoricFomDate, LocalDate maxHistoricFomDate) {
        return historicFomDates(fomDatoer).stream()
            .filter(d -> likEllerFoer(d, maxHistoricFomDate))
            .filter(d -> likEllerEtter(d, minHistoricFomDate))
            .toList();
    }

    protected List<LocalDate> historicFomDates(List<LocalDate> fomDatoer) {
        return fomDatoer.stream()
            .filter(d -> d.isBefore(tidligst(currentFomDate(fomDatoer), LocalDate.now())))
            .toList();
    }

    protected LocalDate currentFomDate(List<LocalDate> fomDatoer) {
        return fomDatoer.stream()
            .filter(d -> likEllerFoer(d, LocalDate.now()))
            .max(LocalDate::compareTo)
            .orElse(null);
    }

    protected List<LocalDate> futureFomDates(List<LocalDate> fomDatoer, List<StillingPeriode> stillingPerioder) {
        LocalDate minFutureFomDate = stillingPerioder.stream().map(StillingPeriode::gyldigFom).filter(d -> d.isAfter(LocalDate.now())).min(LocalDate::compareTo).orElse(null);
        LocalDate maxFutureFomDate = stillingPerioder.stream().map(StillingPeriode::gyldigFom).max(LocalDate::compareTo).orElse(null);
        return futureFomDates(fomDatoer, minFutureFomDate, maxFutureFomDate);
    }

    protected List<LocalDate> futureFomDates(List<LocalDate> fomDatoer, LocalDate minFutureFomDate, LocalDate maxFutureFomDate) {
        return futureFomDates(fomDatoer).stream()
            .filter(d -> likEllerEtter(d, minFutureFomDate))
            .filter(d -> likEllerFoer(d, maxFutureFomDate))
            .toList();
    }

    protected List<LocalDate> futureFomDates(List<LocalDate> fomDatoer) {
        return fomDatoer.stream()
            .filter(d -> d.isAfter(tidligst(currentFomDate(fomDatoer), LocalDate.now())))
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

    private void lagErrorPeriodeUtenStillingPerioder(StrukturerteStillingPerioderForNavident sspForNavident, LocalDate fomDate) {

        List<String> SPer = sspForNavident.stillingPerioder().stream()
            .map(sp -> sp.orgenhet().getId() + "-" + sp.orgenhet().getNavn() + " (" + sp.orgenhet().getAgressoId() + ")" + sp.gyldigFom() + " - " + sp.gyldigTom())
            .toList();

        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.HULL_I_STILLINGSPERIODE,
            """
            Ressurs mangler stilling i periode fra dato %s for Navident %s.
            Dette er stillingperiodene:
            %s
            """
                .formatted(fomDate, sspForNavident.navident(), SPer)
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
            Har StillingsPerioder for følgende orgenhet(er) %s.
            Dette kan være kun fordi navidenten er registrert i Remedy, men ikke har stilling i U4 enda.
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
                .formatted(fom, tom, sspForNavident.navident(), oTer.size(), oTer.stream().map(OrgtilknytningState::fid).toList()),
            stillingPeriode
        );
    }

    private void lagErrorHistoricPeriodeMedFlereOTerSomErDOL(StrukturerteStillingPerioderForNavident sspForNavident, List<StillingPeriode> stillingPerioder) {
        var fom = stillingPerioder.getFirst().gyldigFom();
        var tom = stillingPerioder.getFirst().gyldigTom();

        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.DUPLIKATE_EKSISTERENDE_ORGTILKNYNINGER_MED_DOL_I_HISTORISK_PERIODE,
            """
            I historisk periode [%s - %s] for Navident %s finnes det flere eksisterende OTer som har DOL.
            Dette gjør at vi ikke kan velge StillingPeriode med DOL basert på OT. Vil forsøke å velge basert på Orgenhet På Ressurs i stedet.
            """
                .formatted(fom, tom, sspForNavident.navident())
        );
    }
}
