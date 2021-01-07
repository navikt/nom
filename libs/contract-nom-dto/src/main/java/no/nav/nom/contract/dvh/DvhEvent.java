package no.nav.nom.contract.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class DvhEvent {
    String id;
    long offset;
    @NonNull
    String type;
    JsonNode data;
}
