package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OrgEnhetEndretNavnEventV1 extends OrgEnhetEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgEnhetEndretNavnEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgEnhetNavnEndretEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public OrgEnhetEndretNavnEventV1(String fid, OrgEnhetNavnEndretEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record OrgEnhetNavnEndretEventData(
        String id,
        String navn
    ) implements Data{}
}
