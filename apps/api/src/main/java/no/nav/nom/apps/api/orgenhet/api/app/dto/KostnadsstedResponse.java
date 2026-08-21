package no.nav.nom.apps.api.orgenhet.api.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record KostnadsstedResponse(
    @Schema(requiredMode = REQUIRED)
    String kostnadsstedId,
    @Schema(requiredMode =  REQUIRED)
    String navn,
    @Schema(requiredMode = REQUIRED)
    LocalDate gyldigFom,
    @Schema(requiredMode = REQUIRED)
    LocalDate gyldigTom
) {
}
