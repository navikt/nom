package no.nav.nom.contract.nom.eventsource;


import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.util.Optional;

public class OrganiseringOpprettetEventV1 extends OrganiseringEvent {

    @JsonCreator
    public OrganiseringOpprettetEventV1(String fid, OrganiseringOpprettetEventData data, Metadata metadata, OrganiseringOpprettetAdditionalAgressoData additionalData) {
        super(fid, data, metadata, additionalData);
    }

    public OrganiseringOpprettetEventV1(String fid, OrganiseringOpprettetEventData data, InputMetadata metadata, OrganiseringOpprettetAdditionalAgressoData additionalData) {
        super(fid, data, Metadata.from(metadata), additionalData);
    }

    public record OrganiseringOpprettetEventData(
        String idOver,
        String idUnder,
        String navnOver,
        String navnUnder,
        LocalDate gyldigFom,
        LocalDate gyldigTom
    ) implements Data { }

    public record OrganiseringOpprettetAdditionalAgressoData(
        Optional<String> agressoOrgnodeType,
        String lederNavident
    ) implements OrganiseringEvent.Data {}

}
