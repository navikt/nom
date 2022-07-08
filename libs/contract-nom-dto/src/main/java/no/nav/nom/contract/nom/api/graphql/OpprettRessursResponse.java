package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class OpprettRessursResponse {
    UUID nomId;
    String navIdent;
    String navident;
    String personIdent;
    String personident;
    FolkeregisterPerson person;
    FolkeregisterPerson folkeregisterPerson;
}
