package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PersonResult {

    String personIdent;
    String personident;
    String nomId;
    String navIdent;
    String navident;
    FolkeregisterPerson person;
    FolkeregisterPerson folkeregisterPerson;
}
