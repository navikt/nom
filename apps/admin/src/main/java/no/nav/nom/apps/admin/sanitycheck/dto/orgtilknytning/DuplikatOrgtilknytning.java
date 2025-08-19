package no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;

@Builder
public record DuplikatOrgtilknytning(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid1,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid2,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgEnhetId,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Sektor sektor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean erDagligOppfolging,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Tilknytningtype orgtilknytningType
) {
}
