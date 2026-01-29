package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;

import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.JsonNode;

@Value
@Builder
public class Validation {
    String validationCode;
    String validationDescription;
    JsonNode rawInputData;
}
