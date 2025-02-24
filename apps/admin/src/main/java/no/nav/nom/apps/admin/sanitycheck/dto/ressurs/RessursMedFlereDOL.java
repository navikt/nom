package no.nav.nom.apps.admin.sanitycheck.dto.ressurs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record RessursMedFlereDOL(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String otErMedOverlapp
) {
}
