package no.nav.nom.apps.api.orgtilknytning.api.app;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record MasseFlyttingRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> orgtilknytningerFid,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nyOrgenhetId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean beholdTilganger,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate gyldigFomFlyttedato
) {
    public MasseFlyttingRequest(List<String> orgtilknytningerFid, String nyOrgenhetId, boolean beholdTilganger) {
        this(orgtilknytningerFid, nyOrgenhetId, beholdTilganger, null);
    }
}
