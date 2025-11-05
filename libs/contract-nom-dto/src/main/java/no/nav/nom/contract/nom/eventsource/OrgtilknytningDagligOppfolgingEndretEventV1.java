package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDate;

public class OrgtilknytningDagligOppfolgingEndretEventV1 extends OrgtilknytningEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgtilknytningDagligOppfolgingEndretEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgtilknytningDagligOppfolgingEndretEventData data,
        @JsonProperty("metadata") Metadata metadata,
        @JsonProperty("additionalData") AdditionalAgressoData additionalData
    ) {
        super(fid, data, metadata, additionalData);
    }

    @JsonIgnore
    public OrgtilknytningDagligOppfolgingEndretEventV1(
        String fid,
        OrgtilknytningDagligOppfolgingEndretEventData data,
        InputMetadata inputMetadata,
        OrgtilknytningEvent.AdditionalAgressoData additionalData
    ) {
        this(fid, data, Event.Metadata.from(inputMetadata), additionalData);
    }

    @JsonIgnore
    public OrgtilknytningDagligOppfolgingEndretEventData getOrgtilknytningDagligOppfolgingEndretEventData() {
        return (OrgtilknytningDagligOppfolgingEndretEventData) getData();
    }

    @Builder(toBuilder = true)
    public record OrgtilknytningDagligOppfolgingEndretEventData(
        String navident,
        String orgEnhetId,
        LocalDate gyldigFom,
        LocalDate gyldigTom,
        boolean dagligOppfolging,
        String masseflyttetFraFid
    ) implements Data {
        public OrgtilknytningDagligOppfolgingEndretEventData(
            String navident,
            String orgEnhetId,
            LocalDate gyldigFom,
            LocalDate gyldigTom,
            boolean dagligOppfolging
        ) {
            this(navident, orgEnhetId, gyldigFom, gyldigTom, dagligOppfolging, null);
        }
    }

}