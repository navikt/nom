package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.nom.contract.nom.NomNivaa;

public class NomNivaaEndretEventV1 extends OrgEnhetEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public NomNivaaEndretEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") NomNivaaEndretEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public NomNivaaEndretEventV1(String fid, NomNivaaEndretEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    @JsonIgnore
    public NomNivaaEndretEventData getNomNivaaEndretEventData() {
        return (NomNivaaEndretEventData) getData();
    }

    public record NomNivaaEndretEventData(
        String id,
        NomNivaa nomNivaa
    ) implements Data {}

}
