package no.nav.nom.contract.nom.api.graphql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class OrgEnhetResult {
    ResultCode code;
    String id;  // deprekert jan 2026
    String agressoId;
    String nomId;   // Når deprekert id over er fjernet i fremtiden, kan denne deprekeres, og id bli nom sin interne id (isteden for Agressso ID som den en gang var)
    String orgNiv;  // deprekert jan 2026
    String agressoOrgNiv;
    OrgEnhet organisasjonsenhet;
    OrgEnhet orgEnhet;

    public OrgEnhetResult(OrgEnhet orgEnhet) {
        this.agressoId = orgEnhet.getAgressoInfo().getAgressoId();;
        this.agressoOrgNiv = orgEnhet.getAgressoInfo().getAgressoOrgNiv();
        this.code = ResultCode.OK;
        this.id = orgEnhet.getAgressoInfo().getAgressoId();
        this.nomId = orgEnhet.getId();
        this.orgNiv = orgEnhet.getAgressoInfo().getAgressoOrgNiv();
        this.organisasjonsenhet = orgEnhet;
        this.orgEnhet = orgEnhet;
    }

    public static OrgEnhetResult notFound(String agressoId, String agressoOrgNiv) {
        return new OrgEnhetResult(
            ResultCode.NOT_FOUND,
            agressoId,
            agressoId,
            null,
            agressoOrgNiv,
            agressoOrgNiv,
            null,
            null
        );
    }

}
