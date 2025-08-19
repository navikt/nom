package no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record OverlappendeOrgtilknytning(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident1,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgEnhetId1,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid1,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom1,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate gyldigTom1,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean erDagligOppfolging1,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident2,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgEnhetId2,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid2,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom2,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate gyldigTom2,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean erDagligOppfolging2
) {
}
