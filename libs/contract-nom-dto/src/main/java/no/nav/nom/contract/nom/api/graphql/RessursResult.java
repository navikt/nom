package no.nav.nom.contract.nom.api.graphql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class RessursResult {

    String id;
    Ressurs ressurs;
    ResultCode code;

    public RessursResult(Ressurs ressurs, String id) {
        this.id = id;
        this.ressurs = ressurs;
        this.code = ressurs == null ? ResultCode.NOT_FOUND : ResultCode.OK;
    }
}
