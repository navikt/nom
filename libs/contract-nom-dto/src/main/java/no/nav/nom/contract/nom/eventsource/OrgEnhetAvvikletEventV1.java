package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;

public class OrgEnhetAvvikletEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public OrgEnhetAvvikletEventV1(String fid, OrgEnhetAvvikletEventData data, Event.Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public OrgEnhetAvvikletEventV1(String fid, OrgEnhetAvvikletEventData data, InputMetadata inputMetadata) {
        this(fid, data, Event.Metadata.from(inputMetadata));
    }

    public record OrgEnhetAvvikletEventData(
            LocalDate gyldigTom
    ) implements Data{ }
}