package no.nav.nom.apps.api.orgtilknytning.api.app;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record EndreOrgtilknytningRequest(
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String fid,
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
