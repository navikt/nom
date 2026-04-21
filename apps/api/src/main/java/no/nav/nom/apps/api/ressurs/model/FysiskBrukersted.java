package no.nav.nom.apps.api.ressurs.model;

import jakarta.annotation.Nullable;

import lombok.Builder;

@Builder
public record FysiskBrukersted(
    String nr,
    String navn,
    @Nullable String address,
    @Nullable String postnummer,
    @Nullable String poststed,
    @Nullable String kommune
){}
