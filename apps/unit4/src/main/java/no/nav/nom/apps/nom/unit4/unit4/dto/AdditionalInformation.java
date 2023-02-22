package no.nav.nom.apps.nom.unit4.unit4.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Jacksonized
public class AdditionalInformation{
    private static String FIELD_GRADUATION_YEAR = "graduationYear";
    private static String FIELD_HAS_OVER_TIME = "hasOverTime";
    private static String FIELD_RELATED_PERSON_ID = "relatedPersonId";

    @Builder.Default
    Integer graduationYear = 0; // Feks 2016
    @Builder.Default
    Boolean hasOverTime = true;
    String relatedPersonId;
}
