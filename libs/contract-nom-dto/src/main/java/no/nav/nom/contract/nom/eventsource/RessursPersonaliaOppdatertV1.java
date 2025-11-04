package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RessursPersonaliaOppdatertV1 extends RessursEvent {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RessursPersonaliaOppdatertV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") PersonaliaOppdatertEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public RessursPersonaliaOppdatertV1(String fid, PersonaliaOppdatertEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record PersonaliaOppdatertEventData(
        Visningsnavn visningsnavn
    ) implements Data {}
}
