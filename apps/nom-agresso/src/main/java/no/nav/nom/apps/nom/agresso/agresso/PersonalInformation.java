package no.nav.nom.apps.nom.agresso.agresso;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@Builder
@RequiredArgsConstructor
public class PersonalInformation{
    String gender; // M/F
    String nationalIdentityNo; // FNR
}
