package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class PersonResult {

    String personIdent;
    UUID nomId;
    String navIdent;
    Person person;
}
