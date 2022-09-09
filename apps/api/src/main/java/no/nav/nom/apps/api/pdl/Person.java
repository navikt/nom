package no.nav.nom.apps.api.pdl;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Person {

    private String fornavn;
    private String mellomnavn;
    private String etternavn;
    private Kjonn kjonn;
}
