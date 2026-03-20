package no.nav.nom.apps.api.ressurs.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.nom.contract.nom.state.RessursState;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Ressurs(
    @JsonUnwrapped RessursState state,
    String epost,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Boolean isNomMastered
) {
}
