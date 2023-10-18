package no.nav.nom.apps.api.rest.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StandardResponse {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
}
