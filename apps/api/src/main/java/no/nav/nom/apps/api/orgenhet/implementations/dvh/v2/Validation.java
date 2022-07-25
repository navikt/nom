package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Validation {
    String validationCode;
    String validationDescription;
    JsonNode rawInputData;
}
