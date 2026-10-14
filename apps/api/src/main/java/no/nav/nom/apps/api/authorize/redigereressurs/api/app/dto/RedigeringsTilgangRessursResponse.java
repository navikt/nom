package no.nav.nom.apps.api.authorize.redigereressurs.api.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.nom.contract.nom.state.RessursState;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record RedigeringsTilgangRessursResponse(
    @Schema(requiredMode = REQUIRED)
    String navident,

    @Schema(requiredMode = REQUIRED)
    String visningsnavn,

    @Schema(requiredMode = REQUIRED)
    boolean erDelegert
) {

    public static RedigeringsTilgangRessursResponse createDelegert(RessursState rs) {
        return new RedigeringsTilgangRessursResponse(rs.navident(), rs.visningsnavn().getFormattedVisningsnavn(), true);
    }

    public static RedigeringsTilgangRessursResponse createIkkeDelegert(RessursState rs) {
        return new RedigeringsTilgangRessursResponse(rs.navident(), rs.visningsnavn().getFormattedVisningsnavn(), false);
    }
}
