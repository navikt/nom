package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class RessursSluttetEventV1 extends RessursEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RessursSluttetEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") RessursSluttetEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public RessursSluttetEventV1(String fid, RessursSluttetEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record RessursSluttetEventData(
        LocalDate sluttdato
    ) implements Data {}
}