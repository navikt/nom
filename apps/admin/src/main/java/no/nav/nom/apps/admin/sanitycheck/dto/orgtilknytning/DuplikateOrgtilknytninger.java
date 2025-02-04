package no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning;

import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.nom.contract.nom.state.OrgtilknytningState;

public record DuplikateOrgtilknytninger(
        
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OrgtilknytningState en,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OrgtilknytningState to
) {
}
