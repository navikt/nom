package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.util.Optional;

public class OrgtilknytningFjernetEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public OrgtilknytningFjernetEventV1(String fid, OrgtilknytningFjernetEventData data, Metadata metadata){
        super(fid, data, metadata, null);
    }

    public OrgtilknytningFjernetEventV1(String fid, OrgtilknytningFjernetEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record OrgtilknytningFjernetEventData(
        String navident,
        String orgEnhetId,
        LocalDate gyldigFom,
        Optional<LocalDate> gyldigTom
    ) implements Data{ }

}