package no.nav.nom.apps.api.ressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Set;

@Builder(toBuilder = true)
public record UpdateFysiskeBrukerstederRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    Set<String> fysiskeBrukersteder,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String primaryFysiskBrukersted
) {
}
