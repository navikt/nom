package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Builder
@Value
public class Organisering {
    String id;
    String retning;
    String orgEnhetNr;
    LocalDate gyldigFom;
    LocalDate gyldigTom;
}
