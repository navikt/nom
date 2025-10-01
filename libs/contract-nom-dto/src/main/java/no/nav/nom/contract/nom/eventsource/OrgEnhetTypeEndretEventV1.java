package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.nom.contract.nom.OrgEnhetsType;

public class OrgEnhetTypeEndretEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public OrgEnhetTypeEndretEventV1(String fid, OrgEnhetTypeEndretEventData data, Event.Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public OrgEnhetTypeEndretEventV1(String fid, OrgEnhetTypeEndretEventData data, InputMetadata inputMetadata) {
        this(fid, data, Event.Metadata.from(inputMetadata));
    }

    public OrgEnhetTypeEndretEventData getData() {
        return (OrgEnhetTypeEndretEventData) super.getData();
    }

    public record OrgEnhetTypeEndretEventData(
        OrgEnhetsType orgEnhetsType
    ) implements Data {}

}