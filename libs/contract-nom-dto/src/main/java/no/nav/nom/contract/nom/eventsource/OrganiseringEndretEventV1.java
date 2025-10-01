package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;

public class OrganiseringEndretEventV1  extends OrganiseringEvent{
    @JsonCreator
    public OrganiseringEndretEventV1(String fid, OrganiseringEndretEventData data, Event.Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public OrganiseringEndretEventV1(String fid, OrganiseringEndretEventData data, InputMetadata metadata) {
        super(fid, data, Event.Metadata.from(metadata), null);
    }

    public record OrganiseringEndretEventData(
        String id,
        String navn,
        String idOver,
        String navnOver,
        LocalDate gyldigFom,
        LocalDate gyldigTom
    ) implements Data { }

}
