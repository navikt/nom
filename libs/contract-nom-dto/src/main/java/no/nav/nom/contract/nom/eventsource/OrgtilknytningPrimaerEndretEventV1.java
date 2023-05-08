package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.util.Optional;

public class OrgtilknytningPrimaerEndretEventV1 extends OrgtilknytningEvent {

    @JsonCreator
    public OrgtilknytningPrimaerEndretEventV1(String fid, OrgtilknytningPrimaerEndretEventData data, Metadata metadata, OrgtilknytningEvent.AdditionalAgressoData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public OrgtilknytningPrimaerEndretEventV1(String fid, OrgtilknytningPrimaerEndretEventData data, InputMetadata inputMetadata, OrgtilknytningEvent.AdditionalAgressoData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public record OrgtilknytningPrimaerEndretEventData(
            String navident,
            String orgEnhetId,
            LocalDate gyldigFom,
            Optional<LocalDate> gyldigTom,
            boolean primaer
    ) implements Data{ }

}