package no.nav.nom.apps.api.ressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record AvvikleRessursRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate sluttDato
) { }
