package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrganiseringEndretNavnEventV1 extends OrganiseringEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrganiseringEndretNavnEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrganiseringEndretNavnEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public OrganiseringEndretNavnEventV1(String fid, OrganiseringEndretNavnEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record OrganiseringEndretNavnEventData(
        String id,
        String navn
    ) implements Data {}
}
