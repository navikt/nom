package no.nav.nom.apps.api.rest.dolly;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DollyRessursRequest(
    @NotNull String personident,
    @NotNull String fornavn,
    String mellomnavn,
    @NotNull String etternavn,

    @JsonFormat(pattern = "yyyy-MM-dd") @Schema(description = "'yyyy-MM-dd' - Hvis ikke angitt vil opprettRessurs bruke dagens dato") LocalDate startDato,
    @JsonFormat(pattern = "yyyy-MM-dd") @Schema(description = "'yyyy-MM-dd' - Hvis ikke angitt vil avsluttRessurs bruke dagens dato") LocalDate sluttDato
) {}
