package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.time.LocalDate;

public class OrgtilknytningDatoEndretEventV2 extends OrgtilknytningEvent {

    @JsonCreator
    public OrgtilknytningDatoEndretEventV2(
            String fid,
            OrgtilknytningDatoEndretEventData data,
            Event.Metadata metadata,
            OrgtilknytningEvent.AdditionalAgressoData additionalData
    ) {
        super(fid, data, metadata, additionalData);
    }

    public OrgtilknytningDatoEndretEventV2(String fid, OrgtilknytningDatoEndretEventData data, InputMetadata inputMetadata, OrgtilknytningEvent.AdditionalAgressoData additionalData) {
        this(fid, data, Event.Metadata.from(inputMetadata), additionalData);
    }

    @JsonIgnore
    public OrgtilknytningDatoEndretEventData getOrgtilknytningDatoEndretEventData() {
        return (OrgtilknytningDatoEndretEventData) getData();
    }

    @Builder(toBuilder = true)
    public record OrgtilknytningDatoEndretEventData(
        LocalDate gyldigFom,
        LocalDate gyldigTom
    ) implements Data {}
}
