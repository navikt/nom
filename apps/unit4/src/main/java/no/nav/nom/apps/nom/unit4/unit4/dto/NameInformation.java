package no.nav.nom.apps.nom.unit4.unit4.dto;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@Builder
@RequiredArgsConstructor
public class NameInformation{
    String aliasName; // XXYY, XX er første to bokstaver i fornavn, og YY skal være første to i etternavn
    String firstName;
    String surname;
}
