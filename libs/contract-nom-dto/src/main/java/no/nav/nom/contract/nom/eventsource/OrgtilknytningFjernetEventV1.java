package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.util.Optional;

public class OrgtilknytningFjernetEventV1 extends OrgtilknytningEvent {

    @JsonCreator
    public OrgtilknytningFjernetEventV1(String fid, OrgtilknytningFjernetEventData data, Metadata metadata, OrgtilknytningEvent.AdditionalAgressoData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public OrgtilknytningFjernetEventV1(String fid, OrgtilknytningFjernetEventData data, InputMetadata inputMetadata, OrgtilknytningEvent.AdditionalAgressoData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public record OrgtilknytningFjernetEventData(
        String navident,
        String orgEnhetId,
        LocalDate gyldigFom,
        Optional<LocalDate> gyldigTom
    ) implements Data{ }

}