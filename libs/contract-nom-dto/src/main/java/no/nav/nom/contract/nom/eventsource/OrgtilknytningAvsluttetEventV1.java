package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class OrgtilknytningAvsluttetEventV1 extends OrgtilknytningEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgtilknytningAvsluttetEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgtilknytningAvsluttetEventData data,
        @JsonProperty("metadata") Metadata metadata,
        @JsonProperty("additionalData") AdditionalAgressoData additionalData
    ) {
        super(fid, data, metadata, additionalData);
    }

    @JsonIgnore
    public OrgtilknytningAvsluttetEventV1(
        String fid,
        OrgtilknytningAvsluttetEventData data,
        InputMetadata inputMetadata,
        OrgtilknytningEvent.AdditionalAgressoData additionalData
    ) {
        this(fid, data, Event.Metadata.from(inputMetadata), additionalData);
    }

    public record OrgtilknytningAvsluttetEventData(
        String navident,
        String orgEnhetId,
        LocalDate gyldigTom,
        String masseflyttetFraFid
    ) implements Data {
        public OrgtilknytningAvsluttetEventData(String navident, String orgEnhetId, LocalDate gyldigTom) {
            this(navident, orgEnhetId, gyldigTom, null);
        }
    }

}