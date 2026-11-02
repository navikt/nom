package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class KostnadsstedEndretEventV1 extends OrgEnhetEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public KostnadsstedEndretEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") KostnadsstedEndretEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public KostnadsstedEndretEventV1(String fid, KostnadsstedEndretEventV1.KostnadsstedEndretEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public KostnadsstedEndretEventData getData() {
        return (KostnadsstedEndretEventData) super.getData();
    }

    public record KostnadsstedEndretEventData(
        String id,
        String kostnadsstedId
    ) implements Data {}
}
