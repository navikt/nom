package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class OrganiseringAvsluttetEventV1 extends OrganiseringEvent {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrganiseringAvsluttetEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrganiseringAvsluttetEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public OrganiseringAvsluttetEventV1(String fid, OrganiseringAvsluttetEventData data, InputMetadata metadata) {
        this(fid, data, Metadata.from(metadata));
    }

    public record OrganiseringAvsluttetEventData(
        String id,
        LocalDate gyldigTom
    ) implements Data {}
}
