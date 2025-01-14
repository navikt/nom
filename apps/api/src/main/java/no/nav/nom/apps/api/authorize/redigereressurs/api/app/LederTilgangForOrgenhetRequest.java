package no.nav.nom.apps.api.authorize.redigereressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record LederTilgangForOrgenhetRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String delegertTilNavident,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgenhetId,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate gyldigTom
) { }
