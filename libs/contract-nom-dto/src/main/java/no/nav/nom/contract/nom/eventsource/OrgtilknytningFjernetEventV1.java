package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class OrgtilknytningFjernetEventV1 extends OrgtilknytningEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgtilknytningFjernetEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgtilknytningFjernetEventData data,
        @JsonProperty("metadata") Metadata metadata,
        @JsonProperty("additionalData") AdditionalAgressoData additionalData
    ) {
        super(fid, data, metadata, additionalData);
    }

    @JsonIgnore
    public OrgtilknytningFjernetEventV1(
        String fid,
        OrgtilknytningFjernetEventData data,
        InputMetadata inputMetadata,
        OrgtilknytningEvent.AdditionalAgressoData additionalData
    ) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public record OrgtilknytningFjernetEventData(
        String navident,
        String orgEnhetId,
        LocalDate gyldigFom,
        LocalDate gyldigTom
    ) implements Data {}

}