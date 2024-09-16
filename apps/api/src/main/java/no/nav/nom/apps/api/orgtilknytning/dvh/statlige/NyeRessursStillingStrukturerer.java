package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.NAVIDENT_FINNES_IKKE;

@Component
@Slf4j
public class NyeRessursStillingStrukturerer {
    private final RessursStateRepository ressursStateRepository;
    private final GrupperOgSlaaSammenDvhStillinger grupperOgSlaaSammenDvhStillinger;
    private final MatchStrukturerteStillingPerioderTilNOM matchStillingPerioderTilNomEnheter;
    private final SettDOLPerioderPaaStrukturerteStillinger settDOLPerioderPaaStrukturerteStillinger;

    public NyeRessursStillingStrukturerer(
        RessursStateRepository ressursStateRepository, GrupperOgSlaaSammenDvhStillinger grupperOgSlaaSammenDvhStillinger,
        MatchStrukturerteStillingPerioderTilNOM matchStillingPerioderTilNomEnheter,
        SettDOLPerioderPaaStrukturerteStillinger settDOLPerioderPaaStrukturerteStillinger
    ) {
        this.matchStillingPerioderTilNomEnheter = matchStillingPerioderTilNomEnheter;
        this.ressursStateRepository = ressursStateRepository;
        this.grupperOgSlaaSammenDvhStillinger = grupperOgSlaaSammenDvhStillinger;
        this.settDOLPerioderPaaStrukturerteStillinger = settDOLPerioderPaaStrukturerteStillinger;
    }

    public Map<String, StrukturerteStillingPerioderForNavident> strukturerStilingPerioderPerNavident(List<DvhRessursStilling> dvhRessursStillinger) {
        Map<String, List<DvhRessursStilling>> dvhRessursStillingPerNavident = grupperDvhRessursStillingerPerNavident(dvhRessursStillinger);

        Map<String, StrukturerteStillingPerioderForNavident> stillingPerioderPerNavident = new HashMap<>();
        dvhRessursStillingPerNavident.forEach((navident, navidentensDvhRessursStillinger) -> {
            stillingPerioderPerNavident.put(navident,
                strukturerStillingPerioderFraDvhRessursStillinger(navident, navidentensDvhRessursStillinger)
            );
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

        var behandletResultat =
            settDOLPerioderPaaStrukturerteStillinger.behandle(
                matchStillingPerioderTilNomEnheter.behandle(
                    grupperOgSlaaSammenDvhStillinger.behandle(sspForNavident)
                )
            );

        // TODO: Sjekke for Ressursens SluttDato - ingen StillingPeriode utover dette
        // TODO: Sanity sjekk av StillingPerioder - ikke negative etc
        // TODO: Sjekk mismatch mellom aktiv stilling og enhet på ressurs i DvhRessursStillinger
        // TODO: Sjekk DvhRessursStillinger utover Ressursens periode

        return behandletResultat;
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
}
