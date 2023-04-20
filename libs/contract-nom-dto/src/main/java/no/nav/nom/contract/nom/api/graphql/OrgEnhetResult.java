package no.nav.nom.contract.nom.api.graphql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class OrgEnhetResult {
    ResultCode code;
    String id;
    String orgNiv;
    OrgEnhet organisasjonsenhet;
    OrgEnhet orgEnhet;

    public OrgEnhetResult(OrgEnhet orgEnhet) {
        this.code = ResultCode.OK;
        this.id = orgEnhet.getAgressoId();
        this.orgNiv = orgEnhet.getOrgNiv();
        this.organisasjonsenhet = orgEnhet;
        this.orgEnhet = orgEnhet;
    }

    public OrgEnhetResult(String id, String orgNiv) {
        this.code = ResultCode.NOT_FOUND;
        this.id = id;
        this.orgNiv = orgNiv;
        this.organisasjonsenhet = null;
        this.orgEnhet = null;
    }

}
