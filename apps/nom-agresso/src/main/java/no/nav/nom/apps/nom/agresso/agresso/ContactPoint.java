package no.nav.nom.apps.nom.agresso.agresso;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@Builder
@RequiredArgsConstructor
public class ContactPoint{
    Address address;
    String contactPointType;

    @Value
    @Builder
    @NoArgsConstructor
    @RequiredArgsConstructor
    public static class Address{
        String place = "MÅ ENDRES";
        String postcode = "00000";
    }
}
