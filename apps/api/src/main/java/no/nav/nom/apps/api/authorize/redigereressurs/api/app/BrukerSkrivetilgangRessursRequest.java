package no.nav.nom.apps.api.authorize.redigereressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record BrukerSkrivetilgangRessursRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bruker,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String ressurs
) {
}