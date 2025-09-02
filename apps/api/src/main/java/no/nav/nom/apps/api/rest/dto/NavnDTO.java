package no.nav.nom.apps.api.rest.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NavnDTO {
    @NotNull String fornavn;
    String mellomnavn;
    @NotNull String etternavn;
}
