package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RessursJobbtelefonOppdatertEventV1 extends RessursEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RessursJobbtelefonOppdatertEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") JobbtelefonOppdatertEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public RessursJobbtelefonOppdatertEventV1(String fid, JobbtelefonOppdatertEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record JobbtelefonOppdatertEventData(
        String navident,
        Jobbtelefon jobbtelefon
    ) implements Data {}
}
