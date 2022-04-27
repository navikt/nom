package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class OpprettRessursResponse2 {
    String navident;
    String personident;
    Person person;
    String fornavn;
    String etternavn;
}
