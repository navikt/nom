package no.nav.nom.apps.api.person.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import no.nav.nom.contract.nom.IdentType;

@Data
@Builder
public class GetPersonRequest {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String personident;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private IdentType identType;
}
