package no.nav.nom.apps.api.orgtilknytning.api.app;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MasseFlyttingRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> ressurserNavident,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nyOrgenhetId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean beholdTilganger
) {
}
