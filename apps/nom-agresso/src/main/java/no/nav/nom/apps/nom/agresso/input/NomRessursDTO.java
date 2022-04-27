package no.nav.nom.apps.nom.agresso.input;

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