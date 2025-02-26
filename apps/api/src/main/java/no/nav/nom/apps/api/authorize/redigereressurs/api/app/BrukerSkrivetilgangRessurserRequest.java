package no.nav.nom.apps.api.authorize.redigereressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record BrukerSkrivetilgangRessurserRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bruker,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> ressurser
) {
}