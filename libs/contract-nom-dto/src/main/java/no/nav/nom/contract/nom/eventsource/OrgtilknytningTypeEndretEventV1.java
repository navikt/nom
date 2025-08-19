package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import no.nav.nom.contract.nom.Tilknytningtype;

// Finnes kun én event (event-01HKVS7C954KP32GJAMPJJW128) av den her typen på kafka i dev, ingen i prod. Burde slette event-01HKVS7C954KP32GJAMPJJW128 og fjerne denne klassen når vi har tid.
public class OrgtilknytningTypeEndretEventV1 extends OrgtilknytningEvent {

    @JsonCreator
    public OrgtilknytningTypeEndretEventV1(String fid, OrgtilknytningTypeEndretEventData data, Metadata metadata){
        super(fid, data, metadata, null);
    }

    public OrgtilknytningTypeEndretEventV1(String fid, OrgtilknytningTypeEndretEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
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
