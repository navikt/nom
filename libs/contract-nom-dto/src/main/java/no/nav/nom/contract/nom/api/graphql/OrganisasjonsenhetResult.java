package no.nav.nom.contract.nom.api.graphql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class OrganisasjonsenhetResult {

    ResultCode code;
    String id;
    Organisasjonsenhet organisasjonsenhet;

    public OrganisasjonsenhetResult(Organisasjonsenhet organisasjonsenhet) {
        this.code = ResultCode.OK;
        this.id = organisasjonsenhet.getAgressoId();
        this.organisasjonsenhet = organisasjonsenhet;
    }

    public OrganisasjonsenhetResult(String id) {
        this.code = ResultCode.NOT_FOUND;
        this.id = id;
        this.organisasjonsenhet = null;
    }

}
