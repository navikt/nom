package no.nav.nom.apps.sfintegrasjon.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class StandardResponse {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
}
