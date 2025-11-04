package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RessursTekniskSlettetEventV1 extends RessursEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RessursTekniskSlettetEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") RessursTekniskSlettetEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public RessursTekniskSlettetEventV1(String fid, RessursTekniskSlettetEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record RessursTekniskSlettetEventData() implements Data {}
}