package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RessursPrimaryTelefonnummerOppdatertEventV1 extends RessursEvent {


    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RessursPrimaryTelefonnummerOppdatertEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") PrimaryTelefonnummerOppdatertEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public RessursPrimaryTelefonnummerOppdatertEventV1(String fid, PrimaryTelefonnummerOppdatertEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record PrimaryTelefonnummerOppdatertEventData(
        String primaryTelefonnummer
    ) implements Data {}
}
