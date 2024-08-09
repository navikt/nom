package no.nav.nom.apps.api.common.testidenter;

import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
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
                Optional.empty(),
                Optional.empty(),
                Optional.of(LocalDate.of(2022, 1, 1)),
                Optional.of("2700"),
                List.of("2700"),
                IdentType.STANDARD
            );
        }

        public OrgtilknytningState asOrgtilknytningState() {
            return new OrgtilknytningState(
                orgtilknytningFid(),
                navident,
                orgtilknytning,
                true,
                LocalDate.of(2022, 1, 1),
                Optional.empty(),
                Optional.empty()
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
        public DvhLeder asDvhLeder(OrgenhetService orgenhetService) {
            return new DvhLeder(
                navident,
                orgenhetService.getById(orgenhet).orElseThrow().getAgressoId(),
                orgenhetService.getById(orgenhet).orElseThrow().getOrgNiv(),
                orgenhetService.getById(orgenhet).orElseThrow().getNavn()
            );
        }
    }
}
