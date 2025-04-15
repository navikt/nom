package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;

public class OrgtilknytningOpprettetEventV1 extends OrgtilknytningEvent {

    @JsonCreator
    public OrgtilknytningOpprettetEventV1(String fid, OrgtilknytningOpprettetEventData data, Metadata metadata, OrgtilknytningEvent.AdditionalAgressoData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public OrgtilknytningOpprettetEventV1(String fid, OrgtilknytningOpprettetEventData data, InputMetadata inputMetadata, OrgtilknytningEvent.AdditionalAgressoData additionalData) {
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
    ) implements Data{ }


}