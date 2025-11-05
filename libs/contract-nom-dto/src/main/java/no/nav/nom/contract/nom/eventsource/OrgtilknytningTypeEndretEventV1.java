package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import no.nav.nom.contract.nom.Tilknytningtype;

// Finnes kun én event (event-01HKVS7C954KP32GJAMPJJW128) av den her typen på kafka i dev, ingen i prod. Burde slette event-01HKVS7C954KP32GJAMPJJW128 og fjerne denne klassen når vi har tid.
public class OrgtilknytningTypeEndretEventV1 extends OrgtilknytningEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgtilknytningTypeEndretEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgtilknytningTypeEndretEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public OrgtilknytningTypeEndretEventData getOrgtilknytningTypeEndretEventData() {
        return (OrgtilknytningTypeEndretEventData) getData();
    }

    @Builder(toBuilder = true)
    public record OrgtilknytningTypeEndretEventData(
        Tilknytningtype orgtilknytningType
    ) implements Data {}
}
