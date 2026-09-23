package no.nav.nom.apps.api.orgtilknytning.api.app;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record UpdateFysiskBrukerstedForOrgtilknytningRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> orgtilknytningerFid,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String tilFysiskBrukerstedNr,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fraFysiskBrukerstedNr
) {
}
