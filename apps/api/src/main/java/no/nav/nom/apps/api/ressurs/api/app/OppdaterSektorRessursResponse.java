package no.nav.nom.apps.api.ressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OppdaterSektorRessursResponse {

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String fid;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String navident;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String visningsnavn;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String message;
}