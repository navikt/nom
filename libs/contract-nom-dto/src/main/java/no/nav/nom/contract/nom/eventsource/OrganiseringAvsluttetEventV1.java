package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;

public class OrganiseringAvsluttetEventV1 extends OrganiseringEvent {
    @JsonCreator
    public OrganiseringAvsluttetEventV1(String fid, OrganiseringAvsluttetEventData data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public OrganiseringAvsluttetEventV1(String fid, OrganiseringAvsluttetEventData data, InputMetadata metadata) {
        super(fid, data, Metadata.from(metadata), null);
    }

    public record OrganiseringAvsluttetEventData(
        LocalDate gyldigTom
    ) implements Data { }

}
