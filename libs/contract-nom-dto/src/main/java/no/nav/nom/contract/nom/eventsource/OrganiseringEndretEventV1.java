package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class OrganiseringEndretEventV1 extends OrganiseringEvent {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrganiseringEndretEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrganiseringEndretEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public OrganiseringEndretEventV1(String fid, OrganiseringEndretEventData data, InputMetadata metadata) {
        this(fid, data, Event.Metadata.from(metadata));
    }

    public record OrganiseringEndretEventData(
        String id,
        String navn,
        String idOver,
        String navnOver,
        LocalDate gyldigFom,
        LocalDate gyldigTom
    ) implements Data {}

}
