package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class PersonResult {

    String personIdent;
    String personident;
    UUID nomId;
    String navIdent;
    String navident;
    FolkeregisterPerson person;
    FolkeregisterPerson folkeregisterPerson;
}
