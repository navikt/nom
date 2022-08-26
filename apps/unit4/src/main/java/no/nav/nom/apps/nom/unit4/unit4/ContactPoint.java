package no.nav.nom.apps.nom.unit4.unit4;

import lombok.Builder;
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
    @RequiredArgsConstructor
    public static class Address{
        String place = "MÅ ENDRES";
        String postcode = "00000";
    }
}
