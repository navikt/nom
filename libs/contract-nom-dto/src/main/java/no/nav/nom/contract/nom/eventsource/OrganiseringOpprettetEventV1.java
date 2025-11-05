package no.nav.nom.contract.nom.eventsource;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class OrganiseringOpprettetEventV1 extends OrganiseringEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrganiseringOpprettetEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrganiseringOpprettetEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public OrganiseringOpprettetEventV1(String fid, OrganiseringOpprettetEventData data, InputMetadata metadata) {
        this(fid, data, Metadata.from(metadata));
    }

    public record OrganiseringOpprettetEventData(
        String id,
        String navn,
        String idOver,
        String navnOver,
        LocalDate gyldigFom,
        LocalDate gyldigTom
    ) implements Data { }

}
