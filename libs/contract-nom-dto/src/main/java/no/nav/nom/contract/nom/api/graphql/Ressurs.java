package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Ressurs {
    private String navIdent;
    private String personIdent;
}
