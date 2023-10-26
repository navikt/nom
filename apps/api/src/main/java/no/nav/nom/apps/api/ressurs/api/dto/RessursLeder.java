package no.nav.nom.apps.api.ressurs.api.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.nom.contract.nom.state.RessursState;

public record RessursLeder(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RessursState leder,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String lederForNavident,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean erDagligOppfolging
) {

}
