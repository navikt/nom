package no.nav.nom.apps.nom.unit4.input;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@Builder
@RequiredArgsConstructor
public class NomRessursDTO {
    String navident;
    String personident;
    String fornavn;
    String mellomnavn;
    String etternavn;
}