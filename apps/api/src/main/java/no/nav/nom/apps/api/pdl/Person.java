package no.nav.nom.apps.api.pdl;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class Person {

    private String fornavn;
    private String mellomnavn;
    private String etternavn;
    private Kjonn kjonn;
    private LocalDate foedselsdato;
}
