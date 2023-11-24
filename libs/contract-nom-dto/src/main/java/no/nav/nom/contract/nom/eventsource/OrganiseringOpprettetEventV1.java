package no.nav.nom.contract.nom.eventsource;


import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.util.Optional;

public class OrganiseringOpprettetEventV1 extends OrganiseringEvent {

    @JsonCreator
    public OrganiseringOpprettetEventV1(String fid, OrganiseringOpprettetEventData data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public OrganiseringOpprettetEventV1(String fid, OrganiseringOpprettetEventData data, InputMetadata metadata) {
        super(fid, data, Metadata.from(metadata), null);
    }

    public record OrganiseringOpprettetEventData(
        String id,
        String navn,
        String idOver,
        String navnOver,
        LocalDate gyldigFom,
        Optional<LocalDate> gyldigTom
    ) implements Data { }

}
