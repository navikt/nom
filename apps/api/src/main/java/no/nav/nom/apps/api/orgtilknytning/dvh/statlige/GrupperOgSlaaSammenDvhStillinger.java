package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import io.getunleash.Unleash;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.dayAfter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.etter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.foer;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.isBetween;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.lik;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerEtter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerFoer;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likPeriode;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.overlapsInterval;
import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.Util.erEtterRessursensSluttDato;
import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.Util.erFoerRessursensStartDato;
import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.Util.erLikEllerEtterRessursensSluttDato;

@Component
@Slf4j
public class GrupperOgSlaaSammenDvhStillinger {
    private final RessursStateRepository ressursStateRepository;
    private final Unleash unleash;

    public GrupperOgSlaaSammenDvhStillinger(RessursStateRepository ressursStateRepository, Unleash unleash) {
        this.ressursStateRepository = ressursStateRepository;
        this.unleash = unleash;
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
        LocalDate ressursStartdato = ressurs.startdato();
        LocalDate ressursSluttdato = ressurs.sluttdato();


        // logging if dvh data contains  "Uforutsett behov" og "Vikar" tilknitningstype
        // might need to replace these tilknitningstype types with Midlertidig
        long uforutsettOrVikarCount = grupperteDvhStillinger.keySet().stream()
            .filter(key ->
                key.tilknytningtype.equals(Tilknytningtype.UFORUTSETT_BEHOV) || key.tilknytningtype.equals(Tilknytningtype.VIKAR))
            .count();
        log.info("Dvh data count containing Uforutsett behov or Vikar as tilknitningstype: {}", uforutsettOrVikarCount);

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
            if (nonNull(periodStart)) {
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

        filtrerUtPerioderSomOverlapperPaaSammeEnhet(perioderGruppert);

        return new StrukturerteStillingPerioderForNavident(
            sspForNavident, perioderGruppert.stream()
            .sorted(
                Comparator.comparing(StillingPeriode::gyldigFom)
                    .thenComparing(
                        StillingPeriode::tilknytningtype,
                        Comparator.nullsLast(Comparator.naturalOrder())
                    ))
            .toList()
        );
    }

    private void filtrerUtPerioderSomOverlapperPaaSammeEnhet(List<StillingPeriode> perioderGruppert) {
        if (isNull(perioderGruppert) || perioderGruppert.isEmpty() || isDatoerSammenhengende(perioderGruppert)) return;

        var spMedTilknytningtype = perioderGruppert.stream()
            .filter(sp -> nonNull(sp.tilknytningtype()))
            .sorted(Comparator.comparing(StillingPeriode::gyldigFom))
            .toList();

        var spUtenTilknytningtype = perioderGruppert.stream()
            .filter(sp -> isNull(sp.tilknytningtype()))
            .sorted(Comparator.comparing(StillingPeriode::gyldigFom))
            .collect(Collectors.toList());

        Set<StillingPeriode> perioderSomSkalBeholdes = new HashSet<>();
        LocalDate brekkpunktFom = null;
        LocalDate brekkpunktTom = null;
        StillingPeriode stillingPeriodeBeforeBreak = null;

        if (spUtenTilknytningtype.isEmpty()) return;

        for (StillingPeriode stillingPeriodeMedTilknytning : spMedTilknytningtype) {
            LocalDate stillingMedTilknytningFom = stillingPeriodeMedTilknytning.gyldigFom();
            LocalDate stillingMedTilknytningTom = stillingPeriodeMedTilknytning.gyldigTom();

            if (nonNull(stillingPeriodeBeforeBreak)) {
                spUtenTilknytningtype.remove(stillingPeriodeBeforeBreak);
                stillingPeriodeBeforeBreak = null;
            }

            for (StillingPeriode sp : spUtenTilknytningtype) {
                boolean stillingIsEquals = stillingPeriodeMedTilknytning.stilling().equals(sp.stilling());
                if (lik(sp.gyldigFom(), stillingMedTilknytningFom) && lik(sp.gyldigTom(), stillingMedTilknytningTom) && stillingIsEquals) {
                    continue;
                }
                if (nonNull(brekkpunktFom) && stillingIsEquals && isBetween(stillingMedTilknytningFom, stillingMedTilknytningTom, sp.gyldigTom())) {
                    brekkpunktTom = stillingMedTilknytningFom.minusDays(1);
                    perioderSomSkalBeholdes.add(new StillingPeriode(
                        sp.stilling(),
                        brekkpunktFom,
                        brekkpunktTom,
                        sp.orgenhet(),
                        sp.sektor(),
                        sp.tilknytningtype()
                    ));
                    stillingPeriodeBeforeBreak = sp;
                    brekkpunktFom = null;
                    brekkpunktTom = null;
                    continue;
                }

                if (foer(sp.gyldigFom(), stillingMedTilknytningFom) && etter(sp.gyldigTom(), stillingMedTilknytningTom) && stillingIsEquals) {
                    perioderSomSkalBeholdes.add(new StillingPeriode(
                        sp.stilling(),
                        sp.gyldigFom(),
                        stillingMedTilknytningFom.minusDays(1),
                        sp.orgenhet(),
                        sp.sektor(),
                        sp.tilknytningtype()
                    ));
                    brekkpunktFom = stillingMedTilknytningTom.plusDays(1);
                    if (spMedTilknytningtype.size() == 1) {
                        perioderSomSkalBeholdes.add(new StillingPeriode(
                            sp.stilling(),
                            stillingMedTilknytningTom.plusDays(1),
                            sp.gyldigTom(),
                            sp.orgenhet(),
                            sp.sektor(),
                            sp.tilknytningtype()
                        ));
                        stillingPeriodeBeforeBreak = sp;
                    }
                    continue;
                }

                if (foer(sp.gyldigFom(), stillingMedTilknytningFom) && !likEllerFoer(sp.gyldigTom(), stillingMedTilknytningFom) && stillingIsEquals) {
                    perioderSomSkalBeholdes.add(new StillingPeriode(
                        sp.stilling(),
                        sp.gyldigFom(),
                        stillingMedTilknytningFom.minusDays(1),
                        sp.orgenhet(),
                        sp.sektor(),
                        sp.tilknytningtype()
                    ));
                    stillingPeriodeBeforeBreak = sp;
                } else if (isBetween(stillingMedTilknytningFom, stillingMedTilknytningTom, sp.gyldigFom()) && sp.gyldigTom().isAfter(stillingMedTilknytningTom) && stillingIsEquals) {
                    brekkpunktFom = stillingMedTilknytningTom.plusDays(1);
                    brekkpunktTom = sp.gyldigTom();
                    break;
                } else if (etter(sp.gyldigTom(), stillingMedTilknytningTom) && !foer(stillingMedTilknytningTom, sp.gyldigFom()) && stillingIsEquals) {
                    brekkpunktFom = stillingMedTilknytningTom.plusDays(1);
                    brekkpunktTom = sp.gyldigTom();
                    break;
                }
            }
        }
        if (nonNull(brekkpunktTom) && !spUtenTilknytningtype.isEmpty()) {
            var stillingPeriode = spUtenTilknytningtype.getLast();
            perioderSomSkalBeholdes.add(new StillingPeriode(
                stillingPeriode.stilling(),
                brekkpunktFom,
                brekkpunktTom,
                stillingPeriode.orgenhet(),
                stillingPeriode.sektor(),
                stillingPeriode.tilknytningtype()
            ));
        }
        perioderGruppert.clear();
        perioderSomSkalBeholdes.addAll(spMedTilknytningtype);
        perioderGruppert.addAll(perioderSomSkalBeholdes);
    }

    private boolean isDatoerSammenhengende(List<StillingPeriode> perioderGruppert) {
        perioderGruppert.sort(Comparator.comparing(StillingPeriode::stilling, Comparator.nullsLast(Comparator.comparing(Objects::toString))).thenComparing(StillingPeriode::gyldigFom));
        StillingPeriode previousPeriode = perioderGruppert.getFirst();
        for (StillingPeriode currentPeriode : perioderGruppert) {
            boolean periodeStillingEnhetIsEqual = previousPeriode.stilling().equals(currentPeriode.stilling());
            if (previousPeriode.equals(currentPeriode) && periodeStillingEnhetIsEqual) {
                continue;
            }
            if (likPeriode(previousPeriode, currentPeriode) && periodeStillingEnhetIsEqual) {
                return false;
            }
            if (likEllerFoer(previousPeriode.gyldigFom(), currentPeriode.gyldigFom()) && likEllerEtter(previousPeriode.gyldigTom(), currentPeriode.gyldigTom()) && periodeStillingEnhetIsEqual) {
                return false;
            }

            if (overlapsInterval(previousPeriode.gyldigFom(), previousPeriode.gyldigTom(), currentPeriode) && periodeStillingEnhetIsEqual) {
                return false;
            }

            previousPeriode = currentPeriode;
        }
        return true;
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
        Tilknytningtype tilknytningtype
    ) {}
}
