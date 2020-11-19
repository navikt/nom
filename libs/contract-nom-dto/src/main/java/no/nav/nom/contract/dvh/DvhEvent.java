package no.nav.nom.contract.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DvhEvent {
    String id;
    long offset;
    String type;
    JsonNode data;
}
