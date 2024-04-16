package no.nav.nom.apps.api.authorize.eventsourcing.state;

import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.nom.apps.api.common.DateUtils;

import java.time.LocalDate;

public record LederTilgang(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String til,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String av,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgenhetId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigTom
) {
    public boolean isActive() {
        return DateUtils.isPeriodActive(gyldigFom, gyldigTom);
    }
}
