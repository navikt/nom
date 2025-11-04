package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDate;

public class OrgtilknytningDatoEndretEventV2 extends OrgtilknytningEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgtilknytningDatoEndretEventV2(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgtilknytningDatoEndretEventData data,
        @JsonProperty("metadata") Metadata metadata,
        @JsonProperty("additionalData") AdditionalAgressoData additionalData
    ) {
        super(fid, data, metadata, additionalData);
    }

    @JsonIgnore
    public OrgtilknytningDatoEndretEventV2(
        String fid,
        OrgtilknytningDatoEndretEventData data,
        InputMetadata inputMetadata,
        OrgtilknytningEvent.AdditionalAgressoData additionalData
    ) {
        this(fid, data, Event.Metadata.from(inputMetadata), additionalData);
    }

    @Builder(toBuilder = true)
    public record OrgtilknytningDatoEndretEventData(
        LocalDate gyldigFom,
        LocalDate gyldigTom
    ) implements Data {}
}
