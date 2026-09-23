package no.nav.nom.apps.api.orgenhet.api.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record EndreOpphengOrgEnhetResponse(
    @Schema(requiredMode = REQUIRED)
    String orgenhetId,
    @Schema(requiredMode = REQUIRED)
    String updatedOverenhetId,
    @Schema(requiredMode = REQUIRED)
    LocalDate gyldigFom,
    @Schema(requiredMode = REQUIRED)
    LocalDate gyldigTom
) {
}
