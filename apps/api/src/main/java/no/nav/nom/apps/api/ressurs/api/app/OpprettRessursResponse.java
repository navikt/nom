package no.nav.nom.apps.api.ressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import no.nav.nom.contract.nom.Sektor;

@Data
@Builder
public class OpprettRessursResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String personident;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String navident;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String visningsfornavn;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String visningsetternavn;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Sektor sektor;

}
