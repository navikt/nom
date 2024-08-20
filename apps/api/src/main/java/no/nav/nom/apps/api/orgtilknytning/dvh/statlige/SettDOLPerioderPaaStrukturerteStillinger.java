package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparing;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.NO_END_DATE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.dayAfter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.dayBefore;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.etter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.foer;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerFoer;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.overlapsInterval;
import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.Util.getEarliestDate;
import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.Util.slaaSammenBackToBackPerioderOgSorterAsc;

@Component
@Slf4j
public class SettDOLPerioderPaaStrukturerteStillinger {
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgenhetService orgenhetService;

    public SettDOLPerioderPaaStrukturerteStillinger(OrgtilknytningStateRepository orgtilknytningStateRepository, OrgenhetService orgenhetService) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgenhetService = orgenhetService;
    }

    public void behandle(StrukturerteStillingPerioderForNavident sspForNavident) {
        final List<StillingPeriode> nyeHistoriskeStillingPerioder = new ArrayList<>();
        // StillingPeriodene er sortert på gyldigFom
        sspForNavident.stillingPerioder().stream()
            .filter(StillingPeriode::isHistoricPeriod)
            .forEach(stillingPeriode -> {
                nyeHistoriskeStillingPerioder.addAll(handleMatchMotEksisterendeOtForHistoriskPeriode(sspForNavident, stillingPeriode));
            });
        // nå inneholder nyeStillingPerioder sett som dekke alle historiske, OG der DOL er satt basert på eksisterende OT'ers DOL
        // Sjekk om det er 'huller i den komplette perioden som ikke har DOL periode, og forsøk evt å dekke de med å matche mot OrgenhetPaaRessurs
        var ryddet = slaaSammenBackToBackPerioderOgSorterAsc(nyeHistoriskeStillingPerioder, sspForNavident);
        var perioderMedHullPaaDOL = finnPerioderMedHullPaaDOL(ryddet);
        var perioderUtenHullPaaDOL = allePerioderUtenHullPaaDOL(ryddet);

        final List<StillingPeriode> nyeStillingPerioder = new ArrayList<>(perioderUtenHullPaaDOL);
        perioderMedHullPaaDOL.forEach(
            sp -> nyeStillingPerioder.add(matchDOLMotOrgenhetPaaRessurs(sspForNavident, sp))
        );

        sspForNavident.stillingPerioder().stream()
            .filter(StillingPeriode::isCurrentPeriod)
            .forEach(stillingPeriode -> {
                nyeStillingPerioder.addAll(handleCurrentPeriod(sspForNavident, stillingPeriode));
            });

        sspForNavident.stillingPerioder().stream()
            .filter(StillingPeriode::isFuturePeriod)
            .forEach(stillingPeriode -> {
                nyeStillingPerioder.addAll(handleFuturePeriod(sspForNavident, stillingPeriode));
            });

        // slå sammen perioder etc

        // Sjekk etter overlappende DOL perioder - what to do?
            // Potensiell situasjon
                // SP matcher OrgenhetPaaRessurs, men eksisterende OT er ikke DOL, en annen OT er satt som DOL.
                    // Dette burde dekkes ved at 'neste' SP for tilsvarende OT over vil da matche perioden som DOL
                    // Hvis vi ikke har tilsvarende SP nå for ovenstående eksisterende OT med DOL, så skal jo den game fjernes


        // Sjekk etter huller i DOL perioder

        // Sjekk etter SP'er med DOL == UKJENT

        // Sjekk etter endringer i faktiske StillingPerioder vi får i input mot det nye settet vi har laget med og uten DOL

        sspForNavident.replaceStillingPerioder(nyeStillingPerioder);
    }

    private List<StillingPeriode> allePerioderUtenHullPaaDOL(List<StillingPeriode> ryddet) {
        // TODO:
        return null;
    }

    private List<StillingPeriode> finnPerioderMedHullPaaDOL(List<StillingPeriode> nyeStillingPerioder) {

        // TODO: - finn perioder uten DOL
        // Fjern hull-periodene fra
        return null;
    }

    private List<StillingPeriode> handleMatchMotEksisterendeOtForHistoriskPeriode(StrukturerteStillingPerioderForNavident sspForNavident, StillingPeriode stillingPeriode) {
        List<StillingPeriode> nyeStillingPerioder = new ArrayList<>();

        // Finn eksisterende OT(er) for perioden - og bruk disse sine DOL er.
        var OTer = orgtilknytningStateRepository.findByNavident(sspForNavident.navident()).stream()
            .filter(ot ->
                stillingPeriode.orgenhet().getId().equals(ot.orgEnhetId()) &&
                    overlapsInterval(stillingPeriode, ot)
            )
            .sorted(comparing(OrgtilknytningState::gyldigFom))
            .toList();

        if (OTer.isEmpty()) {
            // Hvis ingen OT er funnet, så match mot OrgenhetPaaRessurs
            nyeStillingPerioder.add(matchDOLMotOrgenhetPaaRessurs(sspForNavident, stillingPeriode));
        } else {
            List<StillingPeriode> restStillingPerioder = new ArrayList<>();
            StillingPeriode currentSP = stillingPeriode;

            for (OrgtilknytningState ot : OTer) {
                if (currentSP == null) {
                    // Finnes flere OTer som overlapper med samme enhet på slutten av perioden - dette er inkonsistens i eksisterende OT'er
                    // Rapporter med Error - og fortsett

                    // TODO: LagError
                    continue;
                }

                if (etter(ot.gyldigFom(), currentSP.gyldigFom())) {
                    // Vi har udekket start på currentSP-periode - splitt ut denne delperioden, og sett den til rest perioder for senere behandling
                    restStillingPerioder.add(new StillingPeriode(currentSP.stilling(), currentSP.gyldigFom(), dayBefore(ot.gyldigFom()), currentSP.orgenhet()));
                    currentSP = new StillingPeriode(currentSP.stilling(), ot.gyldigFom(), currentSP.gyldigTom(), currentSP.orgenhet());
                }

                if (likEllerFoer(ot.gyldigTom().orElse(NO_END_DATE), currentSP.gyldigFom())) {
                    // Finnes flere OTer som overlapper med samme enhet i perioden - dette er inkonsistens i eksisterende OT'er
                    // Vi har allerede dekt duplikat OT sin periode
                    // Rapporter med Error - og fortsett
                    // TODO: LagError
                    continue;
                }
                if (etter(ot.gyldigTom().orElse(null), currentSP.gyldigTom())) {
                    // Finnes flere OTer som overlapper med samme enhet i perioden - dette er inkonsistens i eksisterende OT'er
                    // Rapporter med Error
                    // Denne neste OT'en har lengre gyldigTom enn forrige, så behandle denne delperioden
                    // TODO: lagError
                }

                var delPeriode = new StillingPeriode(
                    currentSP.stilling(),
                    currentSP.gyldigFom(),
                    getEarliestDate(ot.gyldigTom().orElse(NO_END_DATE), currentSP.gyldigTom()),
                    orgenhetService.getById(ot.orgEnhetId()).get(),
                    ot.erDagligOppfolging() ? StillingPeriode.DagligOppfolging.JA : StillingPeriode.DagligOppfolging.NEI
                );

                nyeStillingPerioder.add(delPeriode);

                if (foer(delPeriode.gyldigTom(), currentSP.gyldigTom())) {
                    currentSP = new StillingPeriode(
                        currentSP.stilling(),
                        dayAfter(delPeriode.gyldigTom()),
                        currentSP.gyldigTom(),
                        currentSP.orgenhet());
                } else {
                    currentSP = null; // Vi har behandlet hele denne StillingPerioden
                }
            }

            // Behandle resterende StillingPerioder som ikke overlapper med OT som Match mot OrgenhetPaaRessurs
            restStillingPerioder.forEach(sp -> {
                // TODO: - vi skal ikke behandle bruke denne SP'ens periode til match på OrgenhetPaaRessurs
                //  hvis vi har dekket perioden med DOL på noen av de andre SP'ene...
                //  ...
                //  Hvordan vet vi dette? vi har jo ikke behandlet de andre enda...

                // TODO: Kanskje løp igjennom alle historiske og match på eksisterende OT først
                //  og deretter sjekke manglende hull i DOL perioder mot overlappende SP'er og forsøke match mot OrgenhetPaaRessurs
                //  ^^^^^   ---------   ^^^^^
                nyeStillingPerioder.add(matchDOLMotOrgenhetPaaRessurs(sspForNavident, sp));
            });
        }
        return nyeStillingPerioder;
    }

    private List<StillingPeriode> handleCurrentPeriod(StrukturerteStillingPerioderForNavident sspForNavident, StillingPeriode stillingPeriode) {
        List<StillingPeriode> nyeStillingPerioder = new ArrayList<>();

        return nyeStillingPerioder;
    }

    private List<StillingPeriode> handleFuturePeriod(StrukturerteStillingPerioderForNavident sspForNavident, StillingPeriode stillingPeriode) {
        List<StillingPeriode> nyeStillingPerioder = new ArrayList<>();

        return nyeStillingPerioder;
    }

    private StillingPeriode matchDOLMotOrgenhetPaaRessurs(StrukturerteStillingPerioderForNavident sspForNavident, StillingPeriode stillingPeriode) {
        // TODO: - vi skal ikke behandle bruke denne SP'ens periode til match på OrgenhetPaaRessurs
        //  hvis vi har dekket perioden med DOL på noen av de andre SP'ene....
        return new StillingPeriode(stillingPeriode,
            stillingPeriode.orgenhet().getAgressoId().startsWith(stillingPeriode.stilling().ressursAgressoId().orElse("NULL")) ?
                StillingPeriode.DagligOppfolging.JA : StillingPeriode.DagligOppfolging.NEI);
    }
}
