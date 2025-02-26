package no.nav.nom.apps.api.ressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record UpdatePrimaryFysiskBrukerstedForRessurserRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> navidenter,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String newPrimaryFysiskBrukersted
) {
}
