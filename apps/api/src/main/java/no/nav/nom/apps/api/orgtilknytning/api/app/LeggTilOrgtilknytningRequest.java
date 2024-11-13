package no.nav.nom.apps.api.orgtilknytning.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Builder(toBuilder = true)
public record LeggTilOrgtilknytningRequest(
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String orgEnhetId,
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    Sektor sektor,
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDate gyldigFom,
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    Boolean erDagligOppfolging,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    LocalDate gyldigTom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    Tilknytningtype tilknytningtype
) {
}
