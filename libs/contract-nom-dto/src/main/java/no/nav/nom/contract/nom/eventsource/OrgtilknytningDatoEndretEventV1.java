package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.util.Optional;

public class OrgtilknytningDatoEndretEventV1 extends OrgtilknytningEvent {

    @JsonCreator
    public OrgtilknytningDatoEndretEventV1(String fid, OrgtilknytningDatoEndretEventData data, Metadata metadata, OrgtilknytningEvent.AdditionalAgressoData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public OrgtilknytningDatoEndretEventV1(String fid, OrgtilknytningDatoEndretEventData data, InputMetadata inputMetadata, OrgtilknytningEvent.AdditionalAgressoData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public record OrgtilknytningDatoEndretEventData(
            String navident,
            String orgEnhetId,
            LocalDate gyldigFom,
            Optional<LocalDate> gyldigTom
    ) implements Data{ }
}