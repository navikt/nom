package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.util.Optional;

public class OrgtilknytningDagligOppfolgingEndretEventV1 extends OrgtilknytningEvent {

    @JsonCreator
    public OrgtilknytningDagligOppfolgingEndretEventV1(String fid, OrgtilknytningDagligOppfolgingEndretEventData data, Metadata metadata, OrgtilknytningEvent.AdditionalAgressoData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public OrgtilknytningDagligOppfolgingEndretEventV1(String fid, OrgtilknytningDagligOppfolgingEndretEventData data, InputMetadata inputMetadata, OrgtilknytningEvent.AdditionalAgressoData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public record OrgtilknytningDagligOppfolgingEndretEventData(
            String navident,
            String orgEnhetId,
            LocalDate gyldigFom,
            Optional<LocalDate> gyldigTom,
            boolean DagligOppfolging
    ) implements Data{ }

}