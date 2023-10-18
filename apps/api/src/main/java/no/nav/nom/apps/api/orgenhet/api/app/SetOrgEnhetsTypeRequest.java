package no.nav.nom.apps.api.orgenhet.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import no.nav.nom.contract.nom.OrgEnhetsType;

@Data
@AllArgsConstructor
public class SetOrgEnhetsTypeRequest {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) private String nomId;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) private OrgEnhetsType orgEnhetsType;
}
