package no.nav.nom.apps.api.ressurs.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import jakarta.annotation.Nullable;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Builder
public record FysiskBrukersted(
    @Schema(requiredMode = REQUIRED)
    String nr,

    @Schema(requiredMode = REQUIRED)
    String navn,

    @Nullable String identification,
    @Nullable String address,
    @Nullable String postnummer,
    @Nullable String poststed,
    @Nullable String kommune
){}
