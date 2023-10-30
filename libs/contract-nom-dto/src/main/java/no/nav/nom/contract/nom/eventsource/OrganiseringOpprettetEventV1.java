package no.nav.nom.contract.nom.eventsource;


import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;

public class OrganiseringOpprettetEventV1 extends OrganiseringEvent {

    @JsonCreator
    public OrganiseringOpprettetEventV1(String fid, Data data, Metadata metadata, OrganiseringEvent.Data additionalData) {
        super(fid, data, metadata, additionalData);
    }

    @JsonCreator
    public OrganiseringOpprettetEventV1(String fid, Data data, InputMetadata metadata, OrganiseringEvent.Data additionalData) {
        super(fid, data, Metadata.from(metadata), additionalData);
    }

    public record OrganiseringOpprettetEventData(
        String agressoIdOver,
        String agressoIdUnder,
        String orgNivOver,
        String orgNivUnder,
        String navnOver,
        String navnUnder,
        LocalDate gyldigFom,
        LocalDate gyldigTom
    ) implements Data { }

}
