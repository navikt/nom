package no.nav.nom.contract.nom.api.graphql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class OrgEnhetResult {
    ResultCode code;
    String id;  // Av gammelt er dette agressoId - TODO: Må endres med breaking changes eller via dobbel deprekering
    String nomId;
    String orgNiv;
    OrgEnhet organisasjonsenhet;
    OrgEnhet orgEnhet;

    public OrgEnhetResult(OrgEnhet orgEnhet) {
        this.code = ResultCode.OK;
        this.id = orgEnhet.getAgressoInfo().getAgressoId();
        this.nomId = orgEnhet.getId();
        this.orgNiv = orgEnhet.getAgressoInfo().getAgressoOrgNiv();
        this.organisasjonsenhet = orgEnhet;
        this.orgEnhet = orgEnhet;
    }

    public static OrgEnhetResult notFound(String id, String orgNiv) {
        return new OrgEnhetResult(
            ResultCode.NOT_FOUND,
            id,
            null,
            orgNiv,
            null,
            null
        );
    }

}
