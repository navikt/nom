package no.nav.nom.apps.api.ressurs.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.state.RessursState;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Ressurs(
    @JsonUnwrapped RessursState state,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Telefon> telefoner,
    String epost,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Boolean isNomMastered
) {

    public record Telefon(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nummer,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TelefonType type
    ) {
    }
}
