package no.nav.nom.apps.api.ressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder(toBuilder = true)
public record UpdatePrimaryFysiskBrukerstedRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String newPrimaryFysiskBrukersted
) {
}
