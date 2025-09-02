package no.nav.nom.apps.api.rest.dolly;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import no.nav.nom.apps.api.rest.dto.NavnDTO;
import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;

@Builder
public record RessursDTO(
    @Schema(description = "Intern nøkkel i NOM. 1-til-1 mot navident") @NotNull String fid,
    @NotNull String personident,
    @NotNull String navident,
    @NotNull NavnDTO navn,
    LocalDate startDato,
    LocalDate sluttDato,
    @NotNull Sektor sektor
) {}

