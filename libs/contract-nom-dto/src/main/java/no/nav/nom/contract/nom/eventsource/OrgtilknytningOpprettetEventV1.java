package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;

public class OrgtilknytningOpprettetEventV1 extends OrgtilknytningEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgtilknytningOpprettetEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgtilknytningOpprettetEventData data,
        @JsonProperty("metadata") Metadata metadata,
        @JsonProperty("additionalData") AdditionalAgressoData additionalData
    ) {
        super(fid, data, metadata, additionalData);
    }

    @JsonIgnore
    public OrgtilknytningOpprettetEventV1(
        String fid,
        OrgtilknytningOpprettetEventData data,
        InputMetadata inputMetadata,
        OrgtilknytningEvent.AdditionalAgressoData additionalData
    ) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    @JsonIgnore
    public OrgtilknytningOpprettetEventData getOrgtilknytningOpprettetEventData() {
        return (OrgtilknytningOpprettetEventData) getData();
    }

    @Builder(toBuilder = true)
    public record OrgtilknytningOpprettetEventData(
        String navident,
        String orgEnhetId,
        Sektor sektor,
        LocalDate gyldigFom,
        String masseflyttetFraFid
    ) implements Data {}


}