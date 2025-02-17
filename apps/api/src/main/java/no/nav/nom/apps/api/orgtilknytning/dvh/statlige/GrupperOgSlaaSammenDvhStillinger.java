package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.groupingBy;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.dayAfter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.Util.erEtterRessursensSluttDato;
import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.Util.erFoerRessursensStartDato;
import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.Util.erLikEllerEtterRessursensSluttDato;

@Component
@Slf4j
public class GrupperOgSlaaSammenDvhStillinger {
    private final RessursStateRepository ressursStateRepository;

    public GrupperOgSlaaSammenDvhStillinger(RessursStateRepository ressursStateRepository) {
        this.ressursStateRepository = ressursStateRepository;
    }

    protected StrukturerteStillingPerioderForNavident behandle(StrukturerteStillingPerioderForNavident sspForNavident) {
        // grupper først på Stillingens - stilling enhet/navn +enhetNavn+ressurs (A+B), sektor og tilknytningtype
        var grupperteDvhStillinger = sspForNavident.dvhRessursStillinger().stream()
            .collect(groupingBy(rs ->
                new StillingPeriodeKey(
                    new StillingPeriode.StillingAgressoOrgIdOgNavn(
                        rs.getStillingAgressoId(),
                        rs.getStillingOrgenhetNavn(),
                        rs.getRessursAgressoId(),
                        rs.getRessursOrgenhetNiv()
                    ),
                    rs.getSektor(),
                    rs.getTilknytningtype()
                )
            ));

        List<StillingPeriode> perioderGruppert = new ArrayList<>();

        var ressurs = ressursStateRepository.findByNavident(sspForNavident.navident()).orElseThrow();
        LocalDate ressursStartdato = ressurs.startdato().orElse(null);
        LocalDate ressursSluttdato = ressurs.sluttdato().orElse(null);

        // Sorter og slå sammen sammenhengende perioder
        grupperteDvhStillinger.forEach((stillingPeriodeKey, grupperteRessursStillinger) -> {
            var sorted = grupperteRessursStillinger.stream().sorted(Comparator.comparing(DvhRessursStilling::getStillingDatoFom)).toList();

            LocalDate periodStart = null;
            LocalDate previousEnd = null;

            for (DvhRessursStilling rs : sorted) {
                var start = rs.getStillingDatoFom();
                var end = rs.getStillingDatoTom();

                if (erLikEllerEtterRessursensSluttDato(start, ressursSluttdato)) {
                    lagErrorStartEtterRessursSluttdato(sspForNavident, rs, ressurs.navident(), ressursSluttdato);
                    continue; // Skipp denne DvhRessursStillings raden
                }

                if (erFoerRessursensStartDato(end, ressursStartdato)) {
                    lagErrorEndFoerRessursStartdato(sspForNavident, rs, ressurs.navident(), ressursStartdato);
                    continue; // Skipp denne DvhRessursStillings raden
                }

                if (periodStart == null) {
                    periodStart = start;
                    previousEnd = end;
                }

                if (erEtterRessursensSluttDato(end, ressursSluttdato)) {
                    lagErrorSlutterEtterRessursSluttdato(sspForNavident, rs, ressurs.navident(), ressursSluttdato);
                    if (end.isEqual(previousEnd)) {
                        previousEnd = ressursSluttdato;
                    }
                    end = ressursSluttdato;
                }

                if (erFoerRessursensStartDato(start, ressursStartdato)) {
                    lagErrorStartFoerRessursStartdato(sspForNavident, rs, ressurs.navident(), ressursStartdato);
                    if (start.isEqual(periodStart)) {
                        periodStart = ressursStartdato;
                    }
                    start = ressursStartdato;
                }

                if (end.isEqual(previousEnd)) {
                    // Kan være første DvhRessursStillings rad som er klippet på startdato ift ressursStartdato)
                    continue;
                } else if (start.isEqual(dayAfter(previousEnd))) {
                    //Stillings raden er fortsettelse på forrige rad - slå sammen
                    previousEnd = end;
                } else if (start.isAfter(previousEnd)) {
                    // Her er det brudd i stilling - Lag en StillingPeriode på forrige periode, og fortsett
                    perioderGruppert.add(new StillingPeriode(
                        stillingPeriodeKey.stillingAgressoOrgIdOgNavn,
                        periodStart,
                        previousEnd,
                        null,
                        stillingPeriodeKey.sektor,
                        stillingPeriodeKey.tilknytningtype
                    ));
                    periodStart = start;
                    previousEnd = end;
                } else {
                    // Her er det overlapp i datoer på samme stillingAgressoId, stillingOrgenhetNavn og stillingRessursAgressoId
                    // Logg Error - og klipp bort overlappingen
                    lagErrorOverlappIPeriode(sspForNavident, rs, ressurs.navident(), stillingPeriodeKey.stillingAgressoOrgIdOgNavn, periodStart, previousEnd, start, end);

                    previousEnd = end;
                }
            }

            // Siste RessursStilling er slått sammen - Lag StillingsPeriode, hvis vi har en periode
            if (periodStart != null) {
                perioderGruppert.add(new StillingPeriode(
                    stillingPeriodeKey.stillingAgressoOrgIdOgNavn,
                    periodStart,
                    previousEnd,
                    null,
                    stillingPeriodeKey.sektor,
                    stillingPeriodeKey.tilknytningtype
                ));
            }
        });

        return new StrukturerteStillingPerioderForNavident(sspForNavident, perioderGruppert.stream().sorted(Comparator.comparing(StillingPeriode::gyldigFom)).toList());
    }

    private void lagErrorStartEtterRessursSluttdato(
        StrukturerteStillingPerioderForNavident sspForNavident,
        DvhRessursStilling dvhRessursStilling,
        String navident,
        LocalDate ressursSluttdato
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET,
            """
            DVH RessursStilling for Navident %s starter %s på eller etter ressursens sluttdato %s.
            Behandler ikke denne DVH RessursStillingen.
            """
                .formatted(navident, dvhRessursStilling.getStillingDatoFom(), ressursSluttdato),
            dvhRessursStilling
        );
    }

    private void lagErrorEndFoerRessursStartdato(
        StrukturerteStillingPerioderForNavident sspForNavident,
        DvhRessursStilling dvhRessursStilling,
        String navident,
        LocalDate ressursStartdato
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET,
            """
            DVH RessursStilling for Navident %s slutter %s før ressursens startdato %s.
            Behandler ikke denne DVH RessursStillingen.
            """
                .formatted(navident, dvhRessursStilling.getStillingDatoTom(), ressursStartdato),
            dvhRessursStilling
        );
    }

    private void lagErrorSlutterEtterRessursSluttdato(
        StrukturerteStillingPerioderForNavident sspForNavident,
        DvhRessursStilling dvhRessursStilling,
        String navident,
        LocalDate ressursSluttdato
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET,
            """
            DVH ressursStilling for Navident %s har sluttdato %s etter ressursens sluttdato %s.
            Avslutter stillingen på ressursens sluttdato.
            """
                .formatted(navident, dvhRessursStilling.getStillingDatoTom(),  ressursSluttdato),
            dvhRessursStilling
        );
    }

    private void lagErrorStartFoerRessursStartdato(
        StrukturerteStillingPerioderForNavident sspForNavident,
        DvhRessursStilling dvhRessursStilling,
        String navident,
        LocalDate ressursStartdato
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET,
            """
            DVH RessursStilling for Navident %s slutter %s før ressursens startdato %s.
            Starter stillingen på ressursens startdato.
            """
                .formatted(navident, dvhRessursStilling.getStillingDatoTom(), ressursStartdato),
            dvhRessursStilling
        );
    }

    private void lagErrorOverlappIPeriode(
        StrukturerteStillingPerioderForNavident sspForNavident,
        DvhRessursStilling dvhRessursStilling,
        String navident,
        StillingPeriode.StillingAgressoOrgIdOgNavn stillingAgressoOrgIdOgNavn,
        LocalDate periodStart, LocalDate previousEnd,
        LocalDate start, LocalDate end
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER,
            "Navident %s har overlapp i stillingsdatoer for '%s' -> forrige periode [%s - %s] og denne stillingen [%s - %s]"
                .formatted(navident, stillingAgressoOrgIdOgNavn, periodStart, previousEnd, start, end),
            dvhRessursStilling
        );
    }

    record StillingPeriodeKey(
        StillingPeriode.StillingAgressoOrgIdOgNavn stillingAgressoOrgIdOgNavn,
        Sektor sektor,
        Optional<Tilknytningtype> tilknytningtype
    ) {}
}
