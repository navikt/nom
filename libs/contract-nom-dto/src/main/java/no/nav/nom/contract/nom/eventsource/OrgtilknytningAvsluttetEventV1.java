package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;

public class OrgtilknytningAvsluttetEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public OrgtilknytningAvsluttetEventV1(String fid, OrgtilknytningAvsluttetEventData data, Metadata metadata, OrgtilknytningAvsluttetAgressoData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public OrgtilknytningAvsluttetEventV1(String fid, OrgtilknytningAvsluttetEventData data, InputMetadata inputMetadata, OrgtilknytningAvsluttetAgressoData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public record OrgtilknytningAvsluttetEventData(
        String navident,
        String orgEnhetId,
        LocalDate gyldigFom,
        LocalDate gyldigTom
    ) implements Data{ }

    public record OrgtilknytningAvsluttetAgressoData(
        String hrresStillingRessursEnhet,
        String hrresStillingerPerioderFormatert
    ) implements Data { }
}