package no.nav.nom.apps.nom.unit4.unit4.dto;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@Builder
@RequiredArgsConstructor
public class AdditionalInformation{
    Integer graduationYear = 0; // Feks 2016
    Boolean hasOverTime = true;
    String relatedPersonId;
}
