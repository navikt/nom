package no.nav.nom.apps.nom.unit4.unit4.dto;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@Builder
@RequiredArgsConstructor
public class PersonalInformation{
    Unit4RessursGender gender; // M/F
    String nationalIdentityNo; // FNR
}
