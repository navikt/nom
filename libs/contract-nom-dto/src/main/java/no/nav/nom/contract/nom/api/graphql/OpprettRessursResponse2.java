package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OpprettRessursResponse2 {
    String navident;
    String personident;
    FolkeregisterPerson person;
    FolkeregisterPerson folkeregisterPerson;
    String fornavn;
    String etternavn;
}
