package no.nav.nom.contract.nom.eventsource.orgenhetleder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

public class OrgEnhetLederOpprettetEventV1 extends OrgEnhetLederEvent {
    @JsonCreator
    public OrgEnhetLederOpprettetEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgEnhetLederOpprettetEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata);
    }

    public OrgEnhetLederOpprettetEventV1(String fid, OrgEnhetLederOpprettetEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record OrgEnhetLederOpprettetEventData(
        String navident,
        String orgEnhetId
    ) implements Data {}
}
