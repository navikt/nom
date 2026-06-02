package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TilgangsenhetEndretEventV1 extends OrgEnhetEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TilgangsenhetEndretEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") TilgangsenhetEndretEventData data,
        @JsonProperty("metadata") Event.Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public TilgangsenhetEndretEventV1(String fid, TilgangsenhetEndretEventData data, InputMetadata inputMetadata) {
        this(fid, data, Event.Metadata.from(inputMetadata));
    }

    public TilgangsenhetEndretEventData getData() {
        return (TilgangsenhetEndretEventData) super.getData();
    }

    public record TilgangsenhetEndretEventData(
        String tilgangsenhetId
    ) implements Data {}
}
