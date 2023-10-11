package no.nav.nom.apps.api.orgenhet.api.app;

import lombok.AllArgsConstructor;
import lombok.Data;
import no.nav.nom.contract.nom.OrgEnhetsType;

@Data
@AllArgsConstructor
public class SetOrgEnhetsTypeRequest {
    private String nomId;
    private OrgEnhetsType orgEnhetsType;
}
