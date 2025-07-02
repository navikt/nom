package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import no.nav.nom.apps.api.common.DateUtils;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static no.nav.nom.apps.api.common.DateUtils.isPeriodCoveredByAny;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.NO_END_DATE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.dayAfter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.foer;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerEtter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerFoer;

public class Util {

    public static boolean erLikEllerEtterRessursensSluttDato(LocalDate dato, LocalDate ressursSluttdato) {
        return ressursSluttdato != null && (dato.equals(ressursSluttdato) || dato.isAfter(ressursSluttdato));
    }

    public static boolean erFoerRessursensStartDato(LocalDate dato, LocalDate ressursStartdato) {
        return ressursStartdato != null && dato.isBefore(ressursStartdato);
    }

    public static boolean erEtterRessursensSluttDato(LocalDate dato, LocalDate ressursSluttdato) {
        return ressursSluttdato != null && dato.isAfter(ressursSluttdato);
    }


    public static List<LocalDate> sorterOrgenhetFomDatoerNyesteDatoFoerstPrefererLiktNavn(List<IOrgenhet> orgenheter, String enhetsNavn) {
        var enheterMedLiktNavn = orgenheter.stream().filter(oe -> oe.getNavn().equals(enhetsNavn)).toList();
        var perioderForEnheterMedLiktNavn = enheterMedLiktNavn.stream()
            .map(oe -> new DateUtils.Periode(oe.getGyldigFom(), oe.getGyldigTom().orElse(null)))
            .toList();

        // Ta vekk andre enheter som dekkes av periodene til enheter med likt navn
        var enheterUtenLiktNavnMedPeriodeUtoverLiktNavnPerioder = orgenheter.stream()
            .filter(oe -> !oe.getNavn().equals(enhetsNavn))
            .filter(oe -> !isPeriodCoveredByAny(oe.getGyldigFom(), oe.getGyldigTom().orElse(LocalDate.MAX), perioderForEnheterMedLiktNavn))
            .toList();

        return Stream.concat(enheterMedLiktNavn.stream(), enheterUtenLiktNavnMedPeriodeUtoverLiktNavnPerioder.stream())
            .map(oe ->  oe.getGyldigFom() == null ? NO_END_DATE : oe.getGyldigFom()).sorted(Comparator.reverseOrder()).toList();
    }

    public static List<LocalDate> sorterOrgenhetFomDatoerNyesteDatoFoerst(List<IOrgenhet> orgenheter) {
        return orgenheter.stream().map( oe ->  oe.getGyldigFom() == null ? NO_END_DATE : oe.getGyldigFom()).sorted(Comparator.reverseOrder()).toList();
    }

    public static List<IOrgenhet> sorterMedNyesteEnhetFoerst(List<IOrgenhet> orgenheter) {
        return orgenheter.stream()
            .sorted(Comparator.comparing((IOrgenhet orgenhet) -> orgenhet.getGyldigFom() == null ? NO_END_DATE : orgenhet.getGyldigFom()).reversed()).toList();
    }

    public static boolean overlapperEllerErDirekteEtterfoelgende(LocalDate forrigeTom, LocalDate nesteFom) {
        return likEllerFoer(nesteFom, dayAfter(forrigeTom));
    }

    public static LocalDate getLatestDate(LocalDate d1, LocalDate d2) {
        d1 = d1 == null ? NO_END_DATE : d1;
        d2 = d2 == null ? NO_END_DATE : d2;
        return d1.isBefore(d2) ? d2 : d1;
    }

    public static LocalDate getEarliestDate(LocalDate d1, LocalDate d2) {
        d1 = d1 == null ? NO_END_DATE : d1;
        d2 = d2 == null ? NO_END_DATE : d2;
        return d1.isBefore(d2) ? d1 : d2;
    }

    public static boolean erOrgenhetGyldigIHeleStillingPerioden(StillingPeriode stillingPeriode, IOrgenhet matchetOrgenhet) {
        return likEllerEtter(stillingPeriode.gyldigFom(), matchetOrgenhet.getGyldigFom())
            && likEllerFoer(stillingPeriode.gyldigTom(), matchetOrgenhet.getGyldigTom().orElse(NO_END_DATE));
    }


    public static List<StillingPeriode> slaaSammenBackToBackPerioderOgSorterAsc(
        List<StillingPeriode> stillingPerioder, StrukturerteStillingPerioderForNavident sspForNavident
    ) {
        stillingPerioder = advarOgFjernNegativePerioder(stillingPerioder, sspForNavident);

        List<StillingPeriode> sammenslaatt = new ArrayList<>();
        var gruppertOrgenhetOgDol = stillingPerioder.stream().collect(groupingBy(sp -> sp.orgenhet().getId() + " - " + sp.dagligOppfolging()));

        gruppertOrgenhetOgDol.forEach((OrgenhetOgDol, stillingPerioderForOrgenhetOgDol) -> {
            stillingPerioderForOrgenhetOgDol.sort(Comparator.comparing(StillingPeriode::gyldigFom));
            StillingPeriode forrige = null;
            for (StillingPeriode sp: stillingPerioderForOrgenhetOgDol) {
                if (forrige == null) {
                    forrige = sp;
                } else {
                    if (likEllerFoer(sp.gyldigFom(), dayAfter(forrige.gyldigTom())) && sektorOgTilknytningtypeIsEqual(sp, forrige)) {  // Denne bør fjerne eventuelle overlappende perioder på samme enhet
                        forrige = new StillingPeriode(forrige.stilling(), forrige.gyldigFom(), sp.gyldigTom(), forrige.orgenhet(), forrige.dagligOppfolging(), forrige.sektor(), forrige.tilknytningtype());
                    } else {
                        sammenslaatt.add(forrige);
                        forrige = sp;
                    }
                }
            }
            if (forrige != null) {
                sammenslaatt.add(forrige);
            }
        });

        sammenslaatt.sort(
            Comparator.comparing(StillingPeriode::gyldigFom)
                .thenComparing(StillingPeriode::dagligOppfolging)
                .thenComparing(sp -> sp.orgenhet().getId()));

        return sammenslaatt;
    }

    public static boolean sektorOgTilknytningtypeIsEqual(StillingPeriode sp, StillingPeriode forrige) {
        if (sp.sektor() == null && forrige.sektor() == null) {
            return true;
        }

        if (sp.sektor() == null || forrige.sektor() == null) {
            return false;
        }

        return sp.sektor().equals(forrige.sektor()) && sp.tilknytningtype().equals(forrige.tilknytningtype());
    }

    private static List<StillingPeriode> advarOgFjernNegativePerioder(List<StillingPeriode> stillingPerioder, StrukturerteStillingPerioderForNavident sspForNavident) {
        stillingPerioder.stream()
            .filter(sp -> foer(sp.gyldigTom(), sp.gyldigFom()))
            .forEach(sp -> lagErrorHarNegativStillingPeriode(sspForNavident, sp, sspForNavident.navident()));

        return stillingPerioder.stream()
            .filter(sp -> likEllerEtter(sp.gyldigTom(), sp.gyldigFom()))
            .toList();
    }

    private static void lagErrorHarNegativStillingPeriode(
        StrukturerteStillingPerioderForNavident sspForNavident,
        StillingPeriode stillingPeriode,
        String navident
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.HAR_LAGET_NEGATIV_STILLINGPERIODE__DENNE_FJERNES,
            """
            Har på en eller annen måte laget negativ StillingPeriode for Navident %s med gyldighetsperiode [%s - %s] for enhet [%s (%s) %s]
            Denne fjernes
            """
                .formatted(navident,
                    stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(),
                    stillingPeriode.orgenhet().getId(), stillingPeriode.orgenhet().getNavn(), stillingPeriode.orgenhet().getAgressoId()),
            stillingPeriode
        );
    }
}
