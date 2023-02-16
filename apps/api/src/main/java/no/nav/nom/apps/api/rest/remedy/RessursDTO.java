package no.nav.nom.apps.api.rest.remedy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RessursDTO {

    Navn navn;
    String status;
    String feilmelding;

    @Value
    @Builder
    static class Navn {
        String fornavn;
        String mellomnavn;
        String etternavn;
    }
}

