package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.nom.contract.nom.NomNivaa;

public class NomNivaaEndretEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public NomNivaaEndretEventV1(String fid, NomNivaaEndretEventData data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public NomNivaaEndretEventV1(String fid, NomNivaaEndretEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    @JsonIgnore
    public NomNivaaEndretEventData getNomNivaaEndretEventData() {
        return (NomNivaaEndretEventData) getData();
    }

    public record NomNivaaEndretEventData(
        NomNivaa nomNivaa
    ) implements Data {}

}