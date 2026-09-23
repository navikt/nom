package no.nav.nom.apps.api.orgenhet.api.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record EndreOpphengOrgEnhetRequest(
    @Schema(requiredMode = REQUIRED)
    String orgenhetId,
    @Schema(requiredMode = REQUIRED)
    String currentOverenhetId,
    @Schema(requiredMode = REQUIRED)
    String updatedOverenhetId
) {
}
