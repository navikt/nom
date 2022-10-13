package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.util.Optional;

public class OrgEnhetAvvikletEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public OrgEnhetAvvikletEventV1(String fid, OrgEnhetAvvikletEventData data, Metadata metadata){
        super(fid, data, metadata, null);
    }

    public OrgEnhetAvvikletEventV1(String fid, OrgEnhetAvvikletEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record OrgEnhetAvvikletEventData(
            LocalDate gyldigTom
    ) implements Data{ }
}