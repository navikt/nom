package no.nav.nom.apps.api.rest.person;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PdlPersonDTO {

    Navn navn;

    @Value
    @Builder
    static class Navn {
        String fornavn;
        String mellomnavn;
        String etternavn;
    }
}
