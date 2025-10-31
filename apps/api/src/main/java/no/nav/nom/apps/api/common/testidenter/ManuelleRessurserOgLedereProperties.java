package no.nav.nom.apps.api.common.testidenter;

import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ConfigurationProperties("app.manuelle-ressurser-og-ledere")
public record ManuelleRessurserOgLedereProperties(
    boolean enabled,
    List<ManuellRessurs> ressurser,
    List<ManuellLeder> ledere
) {
    public record ManuellRessurs(
        String navident,
        String orgtilknytning,
        String sektor
    ) {
        public RessursState asState() {
            return new RessursState(
                "FID_MANUELL_" + navident,
                navident,
                navident,
                Sektor.valueOf(sektor),
                new Visningsnavn("Test", Optional.empty(), navident),
                new Jobbtelefon(null, null),
                null,
                null,
                LocalDate.of(2022, 1, 1),
                "2700",
                List.of("2700"),
                IdentType.STANDARD,
                "nom-api"
            );
        }

        public OrgtilknytningState asOrgtilknytningState() {
            return new OrgtilknytningState(
                orgtilknytningFid(),
                navident,
                orgtilknytning,
                Sektor.valueOf(sektor),
                true,
                LocalDate.of(2022, 1, 1),
                null,
                null,
                null,
                "nom-api"
            );
        }

        public String orgtilknytningFid() {
            return "FID_MANUELL_OT_" + navident;
        }
    }

    public record ManuellLeder(
        String navident,
        String orgenhet
    ) {
        public OrgEnhetLederState asState() {
            return OrgEnhetLederState.builder()
                .fid("FID_MANUELL_OT_" + navident + " " + orgenhet)
                .orgEnhetId(orgenhet)
                .navident(navident)
                .startdato(LocalDate.now().minusMonths(1))
                .build();
        }

    }
}
