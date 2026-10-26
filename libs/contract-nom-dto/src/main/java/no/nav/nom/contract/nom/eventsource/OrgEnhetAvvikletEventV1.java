package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class OrgEnhetAvvikletEventV1 extends OrgEnhetEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgEnhetAvvikletEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgEnhetAvvikletEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public OrgEnhetAvvikletEventV1(String fid, OrgEnhetAvvikletEventData data, InputMetadata inputMetadata) {
        this(fid, data, Event.Metadata.from(inputMetadata));
    }

    public record OrgEnhetAvvikletEventData(
        String id,
        LocalDate gyldigTom
    ) implements Data {}
}
