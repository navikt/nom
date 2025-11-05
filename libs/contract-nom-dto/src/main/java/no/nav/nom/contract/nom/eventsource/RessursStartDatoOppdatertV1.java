package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class RessursStartDatoOppdatertV1 extends RessursEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RessursStartDatoOppdatertV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") StartDatoOppdatertEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public RessursStartDatoOppdatertV1(String fid, StartDatoOppdatertEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record StartDatoOppdatertEventData(
        LocalDate startdato
    ) implements Data {}
}
