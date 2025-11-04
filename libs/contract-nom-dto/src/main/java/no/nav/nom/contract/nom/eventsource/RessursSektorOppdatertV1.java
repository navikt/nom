package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.nom.contract.nom.Sektor;

public class RessursSektorOppdatertV1 extends RessursEvent {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RessursSektorOppdatertV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") SektorOppdatertEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public RessursSektorOppdatertV1(String fid, SektorOppdatertEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record SektorOppdatertEventData(
        Sektor sektor
    ) implements Data {}
}
