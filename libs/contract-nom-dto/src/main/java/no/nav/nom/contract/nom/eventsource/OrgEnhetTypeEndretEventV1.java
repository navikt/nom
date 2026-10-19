package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.nom.contract.nom.OrgEnhetsType;

public class OrgEnhetTypeEndretEventV1 extends OrgEnhetEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgEnhetTypeEndretEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgEnhetTypeEndretEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public OrgEnhetTypeEndretEventV1(String fid, OrgEnhetTypeEndretEventData data, InputMetadata inputMetadata) {
        this(fid, data, Event.Metadata.from(inputMetadata));
    }

    public OrgEnhetTypeEndretEventData getData() {
        return (OrgEnhetTypeEndretEventData) super.getData();
    }

    public record OrgEnhetTypeEndretEventData(
        String id,
        OrgEnhetsType orgEnhetsType
    ) implements Data {}

}
