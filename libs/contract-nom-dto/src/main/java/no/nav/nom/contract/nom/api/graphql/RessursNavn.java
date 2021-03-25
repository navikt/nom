package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RessursNavn {
    String fornavn;
    String mellomnavn;
    String etternavn;
}
