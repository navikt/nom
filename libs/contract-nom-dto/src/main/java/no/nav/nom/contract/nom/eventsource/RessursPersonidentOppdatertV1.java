package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RessursPersonidentOppdatertV1 extends RessursEvent {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RessursPersonidentOppdatertV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") PersonidentOppdatertEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public RessursPersonidentOppdatertV1(String fid, PersonidentOppdatertEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record PersonidentOppdatertEventData(
        String personident
    ) implements Data {}
}
