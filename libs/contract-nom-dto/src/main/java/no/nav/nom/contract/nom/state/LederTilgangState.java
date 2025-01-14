package no.nav.nom.contract.nom.state;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record LederTilgangState(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String til,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String av,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgenhetId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate gyldigTom
) {
    public boolean isActive() {
        return (gyldigFom == null || gyldigFom.minusDays(1).isBefore(LocalDate.now())) &&
            (gyldigTom == null || gyldigTom.plusDays(1).isAfter(LocalDate.now()));
    }
}