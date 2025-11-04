package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

public class RessursFysiskeBrukerstederOppdatertEventV1 extends RessursEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RessursFysiskeBrukerstederOppdatertEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") RessursFysiskeBrukerstederOppdatertEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public RessursFysiskeBrukerstederOppdatertEventV1(String fid, RessursFysiskeBrukerstederOppdatertEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    @Builder(toBuilder = true)
    public record RessursFysiskeBrukerstederOppdatertEventData(
        List<String> fysiskeBrukersteder,
        String primaryFysiskBrukersted
    ) implements Data {}
}
