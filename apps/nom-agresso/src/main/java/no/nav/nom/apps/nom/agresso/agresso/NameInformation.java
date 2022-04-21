package no.nav.nom.apps.nom.agresso.agresso;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@Builder
@RequiredArgsConstructor
public class NameInformation{
    String aliasName;
    String firstName;
    String surname;
}
