package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;

public class OrgtilknytningAvsluttetEventV1 extends OrgtilknytningEvent {

    @JsonCreator
    public OrgtilknytningAvsluttetEventV1(String fid, OrgtilknytningAvsluttetEventData data, Metadata metadata, OrgtilknytningEvent.AdditionalAgressoData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public OrgtilknytningAvsluttetEventV1(String fid, OrgtilknytningAvsluttetEventData data, InputMetadata inputMetadata, OrgtilknytningEvent.AdditionalAgressoData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public record OrgtilknytningAvsluttetEventData (
        String navident,
        String orgEnhetId,
        LocalDate gyldigTom
    ) implements Data{ }

}