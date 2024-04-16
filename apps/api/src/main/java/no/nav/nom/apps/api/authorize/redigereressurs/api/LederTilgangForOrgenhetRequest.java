package no.nav.nom.apps.api.authorize.redigereressurs.api;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.FutureOrPresent;

import jakarta.validation.constraints.NotNull;

import lombok.Builder;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record LederTilgangForOrgenhetRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String delegertTilNavident,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgenhetId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @FutureOrPresent LocalDate gyldigTom
) { }
