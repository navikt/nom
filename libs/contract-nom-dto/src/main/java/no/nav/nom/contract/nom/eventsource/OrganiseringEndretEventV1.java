package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.util.Optional;

public class OrganiseringEndretEventV1  extends OrganiseringEvent{
    @JsonCreator
    public OrganiseringEndretEventV1(String fid, OrganiseringEndretEventData data, Metadata metadata, OrganiseringEndretAdditionalAgressoData additionalData) {
        super(fid, data, metadata, additionalData);
    }

    public OrganiseringEndretEventV1(String fid, OrganiseringEndretEventData data, InputMetadata metadata, OrganiseringEndretAdditionalAgressoData additionalData) {
        super(fid, data, Metadata.from(metadata), additionalData);
    }

    public record OrganiseringEndretEventData(
        String idOver,
        String id,
        String navnOver,
        String navn,
        LocalDate gyldigFom,
        LocalDate gyldigTom
    ) implements Data { }

    public record OrganiseringEndretAdditionalAgressoData(
        Optional<String> agressoOrgnodeType,
        String lederNavident
    ) implements OrganiseringEvent.Data {}
}
