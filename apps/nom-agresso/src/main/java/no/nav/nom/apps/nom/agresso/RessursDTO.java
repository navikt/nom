package no.nav.nom.apps.nom.agresso;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@Builder
@RequiredArgsConstructor
public class RessursDTO {
    String fornavn;
    String etternavn;
}
