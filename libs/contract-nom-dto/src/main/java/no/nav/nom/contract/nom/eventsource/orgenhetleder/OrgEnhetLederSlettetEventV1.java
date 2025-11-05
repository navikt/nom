package no.nav.nom.contract.nom.eventsource.orgenhetleder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

public class OrgEnhetLederSlettetEventV1 extends OrgEnhetLederEvent {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgEnhetLederSlettetEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgEnhetLederSlettetEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata);
    }

    @JsonIgnore
    public OrgEnhetLederSlettetEventV1(String fid, OrgEnhetLederSlettetEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record OrgEnhetLederSlettetEventData() implements Data {}
}
