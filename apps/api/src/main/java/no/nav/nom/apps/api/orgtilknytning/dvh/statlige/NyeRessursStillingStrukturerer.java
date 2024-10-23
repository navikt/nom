package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.etter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.foer;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.NAVIDENT_FINNES_IKKE;

@Component
@Slf4j
public class NyeRessursStillingStrukturerer {
    private final RessursStateRepository ressursStateRepository;
    private final GrupperOgSlaaSammenDvhStillinger grupperOgSlaaSammenDvhStillinger;
    private final MatchStrukturerteStillingPerioderTilNOM matchStillingPerioderTilNomEnheter;
    private final SettDOLPerioderPaaStrukturerteStillinger settDOLPerioderPaaStrukturerteStillinger;
    private final NomMastered nomMastered;

    public NyeRessursStillingStrukturerer(
        RessursStateRepository ressursStateRepository, GrupperOgSlaaSammenDvhStillinger grupperOgSlaaSammenDvhStillinger,
        MatchStrukturerteStillingPerioderTilNOM matchStillingPerioderTilNomEnheter,
        SettDOLPerioderPaaStrukturerteStillinger settDOLPerioderPaaStrukturerteStillinger, NomMastered nomMastered
    ) {
        this.matchStillingPerioderTilNomEnheter = matchStillingPerioderTilNomEnheter;
        this.ressursStateRepository = ressursStateRepository;
        this.grupperOgSlaaSammenDvhStillinger = grupperOgSlaaSammenDvhStillinger;
        this.settDOLPerioderPaaStrukturerteStillinger = settDOLPerioderPaaStrukturerteStillinger;
        this.nomMastered = nomMastered;
    }

    public Map<String, StrukturerteStillingPerioderForNavident> strukturerStilingPerioderPerNavident(List<DvhRessursStilling> dvhRessursStillinger) {
        Map<String, List<DvhRessursStilling>> dvhRessursStillingPerNavident = grupperDvhRessursStillingerPerNavident(dvhRessursStillinger);

        Map<String, StrukturerteStillingPerioderForNavident> stillingPerioderPerNavident = new HashMap<>();
        dvhRessursStillingPerNavident.forEach((navident, navidentensDvhRessursStillinger) -> {
            if (nomMastered.isMastered(navident)) {
                log.debug("Navident {} er mastered. Vi behandler ikke orgtilknytning fra DVH for denne", navident);
                return;
            }

            var sspForNavident = strukturerStillingPerioderFraDvhRessursStillinger(navident, navidentensDvhRessursStillinger);
            if (sspForNavident != null) {
                stillingPerioderPerNavident.put(navident, sspForNavident);
            }
        });

        // stillingPerioderPerNavident har nå alle stillingPerioder for alle navidenter
        // Hvor sammenhengende perioder er slått sammen, korrekt NOM orgenhet er populert, og DOL er plassert, og evt feil og problemer identifisert
        return stillingPerioderPerNavident;
    }

    protected Map<String, List<DvhRessursStilling>> grupperDvhRessursStillingerPerNavident(List<DvhRessursStilling> dvhRessursStillinger) {
        return dvhRessursStillinger.stream()
            .filter(rs -> rs.getNavident().isPresent())
            .collect(groupingBy(rs -> rs.getNavident().get()));
    }


    protected StrukturerteStillingPerioderForNavident strukturerStillingPerioderFraDvhRessursStillinger(String navident, List<DvhRessursStilling> dvhRessursStillinger) {
        var sspForNavident = new StrukturerteStillingPerioderForNavident(navident, dvhRessursStillinger);

        if (!finnesNavidentINOM(sspForNavident)) {
            return sspForNavident;
        }
        if (!erStatlig(sspForNavident)) {
            return null;
        }

        var behandletResultat =
            settDOLPerioderPaaStrukturerteStillinger.behandle(
                matchStillingPerioderTilNomEnheter.behandle(
                    grupperOgSlaaSammenDvhStillinger.behandle(sspForNavident)
                )
            );

        sjekkKonsistensPaaResultat(behandletResultat);

        return behandletResultat;
    }

    private void sjekkKonsistensPaaResultat(StrukturerteStillingPerioderForNavident sspForNavident) {
        sjekkStillingPerioderMotRessursensPeriode(sspForNavident);
        sjekkStillingPerioderMotOrgenhetensPeriode(sspForNavident);
        sjekkOverlappendeDOLPerioder(sspForNavident);
        sjekkKonsistensPaaStillingperioder(sspForNavident);
        sjekkMismatchPaaAktivDOLStillingOgOrgEnhetPaaRessurs(sspForNavident);
    }

    private void sjekkStillingPerioderMotRessursensPeriode(StrukturerteStillingPerioderForNavident sspForNavident) {
        var ressurs = ressursStateRepository.findByNavident(sspForNavident.navident()).orElseThrow();
        var ressursStart = ressurs.startdato().orElse(LocalDate.of(1900, 1,1));
        var ressursSlutt = ressurs.sluttdato().orElse(null);
        sspForNavident.stillingPerioder().forEach(sp -> {
            if (foer(sp.gyldigFom(), ressursStart) || etter(sp.gyldigTom(), ressursSlutt)) {
                sspForNavident.addError(
                    StrukturerteStillingPerioderForNavident.ErrorType.STILLINGPERIODE_ER_UTENFOR_RESSURSENS_PERIODE,
                    """
                    Etter behandling av DvhStillinger til behandlede StillingPerioder:
                    StillingsPeriode for Navident %s med gyldighetsperiode [%s - %s] går utover ressursens gyldighetsperiode [%s - %s].
                    """.formatted(
                        sspForNavident.navident(), sp.gyldigFom(), sp.gyldigTom(), ressursStart, ressursSlutt
                    ),
                    sp
                );
            }
        });
    }

    private void sjekkStillingPerioderMotOrgenhetensPeriode(StrukturerteStillingPerioderForNavident sspForNavident) {
        sspForNavident.stillingPerioder().forEach(sp -> {
            if (foer(sp.gyldigFom(), sp.orgenhet().getGyldigFom()) || etter(sp.gyldigTom(), sp.orgenhet().getGyldigTom().orElse(null))) {
                sspForNavident.addError(
                    StrukturerteStillingPerioderForNavident.ErrorType.STILLINGPERIODE_ER_UTENFOR_ORGENHET_PERIODE,
                    """
                    Etter behandling av DvhStillinger til behandlede StillingPerioder:
                    StillingsPeriode for Navident %s med gyldighetsperiode [%s - %s] går utover orgenhetens gyldighetsperiode [%s - %s].
                    """.formatted(
                        sspForNavident.navident(), sp.gyldigFom(), sp.gyldigTom(), sp.orgenhet().getGyldigFom(), sp.orgenhet().getGyldigTom().orElse(null)
                    ),
                    sp
                );
            }
        });
    }

    private void sjekkOverlappendeDOLPerioder(StrukturerteStillingPerioderForNavident sspForNavident) {
        sspForNavident.stillingPerioder().stream()
            .filter(StillingPeriode::erDagligOppfolging)
            .forEach(sp ->
                sspForNavident.stillingPerioder().stream()
                    .filter(StillingPeriode::erDagligOppfolging)
                    .filter(sp2 -> sp != sp2)
                    .filter(sp2 -> PeriodeUtil.overlapsInterval(sp, sp2))
                    .forEach(sp2 ->
                            sspForNavident.addError(
                                StrukturerteStillingPerioderForNavident.ErrorType.STILLINGPERIODER_HAR_OVERLAPPENDE_DOL_PERIODER,
                                """
                                Etter behandling av DvhStillinger til behandlede StillingPerioder:
                                DOL StillingsPeriode for Navident %s med gyldighetsperiode [%s - %s] for enhet (%s %s)
                                overlapper med en annen DOL stillingsperiode med gyldighetsperiode [%s - %s] for enhet (%s %s).
                                """.formatted(
                                    sspForNavident.navident(),
                                    sp.gyldigFom(), sp.gyldigTom(), sp.orgenhet().getId(), sp.orgenhet().getNavn(),
                                    sp2.gyldigFom(), sp2.gyldigTom(), sp2.orgenhet().getId(), sp2.orgenhet().getNavn()
                                ),
                                sp
                            )
                    )
           );
    }

    private void sjekkKonsistensPaaStillingperioder(StrukturerteStillingPerioderForNavident sspForNavident) {
        //Negative perioder
        sspForNavident.stillingPerioder().forEach(sp -> {
            if (foer(sp.gyldigTom(), sp.gyldigFom())) {
                sspForNavident.addError(
                    StrukturerteStillingPerioderForNavident.ErrorType.STILLINGPERIODE_HAR_NEGATIV_PERIODE,
                    """
                    Etter behandling av DvhStillinger til behandlede StillingPerioder:
                    StillingsPeriode for Navident %s med gyldighetsperiode [%s - %s] er negativ.
                    Denne skulle vært fjernet i behandlingen - sjekk for kodefeil...
                    """.formatted(
                        sspForNavident.navident(), sp.gyldigFom(), sp.gyldigTom()
                    ),
                    sp
                );
            }
        });
    }

    private void sjekkMismatchPaaAktivDOLStillingOgOrgEnhetPaaRessurs(StrukturerteStillingPerioderForNavident sspForNavident) {
        sspForNavident.stillingPerioder().stream()
            .filter(StillingPeriode::isCurrentPeriod)
            .filter(StillingPeriode::erDagligOppfolging)
            .forEach(sp -> {
                if (!sp.orgenhet().getAgressoId().startsWith(sp.stilling().ressursAgressoId().orElse(""))) {
                    sspForNavident.addError(
                        StrukturerteStillingPerioderForNavident.ErrorType.AKTIV_DOL_MATCHER_IKKE_ORGENHET_PAA_RESSURS,
                        """
                        Etter behandling av DvhStillinger til behandlede StillingPerioder:
                        Aktiv DOL stillingsperiode for Navident %s med gyldighetsperiode [%s - %s] for enhet (%s %s) med agressoId [%s]
                        matcher ikke 'Orgenhet på ressurs' [%s].
                        """.formatted(
                            sspForNavident.navident(), sp.gyldigFom(), sp.gyldigTom(),
                            sp.orgenhet().getId(), sp.orgenhet().getNavn(), sp.orgenhet().getAgressoId(),
                            sp.stilling().ressursAgressoId().orElse("")
                        ),
                        sp
                    );
                }
        });
    }

    protected boolean finnesNavidentINOM(StrukturerteStillingPerioderForNavident sspForNavident) {
        String navident = sspForNavident.navident();
        if (ressursStateRepository.findByNavident(navident).isEmpty()) {
            log.error("Har ressursStillinger fra DVH for navident {} som ikke finnes i NOM", navident);
            sspForNavident.addError(
                NAVIDENT_FINNES_IKKE,
                "Har ressursStillinger fra DVH for navident %s som ikke finnes i NOM".formatted(navident)
            );
            return false;
        }
        return true;
    }

    private boolean erStatlig(StrukturerteStillingPerioderForNavident sspForNavident) {
        return ressursStateRepository.findByNavident(sspForNavident.navident()).orElseThrow().isStatlig();
    }

}
