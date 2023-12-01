package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Optional;

public class OrgtilknytningDatoEndretEventV2 extends OrgtilknytningEvent {

    @JsonCreator
    public OrgtilknytningDatoEndretEventV2(String fid, OrgtilknytningDatoEndretEventData data, Metadata metadata, OrgtilknytningEvent.AdditionalAgressoData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public OrgtilknytningDatoEndretEventV2(String fid, OrgtilknytningDatoEndretEventData data, InputMetadata inputMetadata, OrgtilknytningEvent.AdditionalAgressoData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    @JsonIgnore
    public OrgtilknytningDatoEndretEventData getOrgtilknytningDatoEndretEventData() {
        return (OrgtilknytningDatoEndretEventData) getData();
    }

    @Builder(toBuilder = true)
    public record OrgtilknytningDatoEndretEventData(
        LocalDate gyldigFom,
        Optional<LocalDate> gyldigTom
    ) implements Data {}
}
