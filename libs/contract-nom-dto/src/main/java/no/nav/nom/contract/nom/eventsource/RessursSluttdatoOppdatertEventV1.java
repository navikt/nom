package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class RessursSluttdatoOppdatertEventV1 extends RessursEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RessursSluttdatoOppdatertEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") RessursSluttdatoOppdatertEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public RessursSluttdatoOppdatertEventV1(String fid, RessursSluttdatoOppdatertEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record RessursSluttdatoOppdatertEventData(
        LocalDate sluttdato
    ) implements Data {}
}
