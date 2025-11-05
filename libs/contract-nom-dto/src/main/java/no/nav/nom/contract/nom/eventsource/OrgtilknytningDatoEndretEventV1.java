package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.Optional;

public class OrgtilknytningDatoEndretEventV1 extends OrgtilknytningEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgtilknytningDatoEndretEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgtilknytningDatoEndretEventData data,
        @JsonProperty("metadata") Metadata metadata,
        @JsonProperty("additionalData") AdditionalAgressoData additionalData
    ) {
        super(fid, data, metadata, additionalData);
    }

    public record OrgtilknytningDatoEndretEventData(
        String navident,
        String orgEnhetId,
        LocalDate gyldigFom,
        Optional<LocalDate> gyldigTom
    ) implements Data {}
}