package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.util.Optional;

public class OrgEnhetTypeEndretEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public OrgEnhetTypeEndretEventV1(String fid, OrgEnhetTypeEndretEventData data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public OrgEnhetTypeEndretEventV1(String fid, OrgEnhetTypeEndretEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record OrgEnhetTypeEndretEventData(
        Optional<OrgEnhetsType> orgEnhetsType
    ) implements Data {}

}