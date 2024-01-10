package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import no.nav.nom.contract.nom.OrgtilknytningType;

import java.util.Optional;

public class OrgtilknytningTypeEndretEventV1 extends OrgtilknytningEvent {

    @JsonCreator
    public OrgtilknytningTypeEndretEventV1(String fid, OrgtilknytningTypeEndretEventData data, Metadata metadata){
        super(fid, data, metadata, null);
    }

    public OrgtilknytningTypeEndretEventV1(String fid, OrgtilknytningTypeEndretEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    @JsonIgnore
    public OrgtilknytningTypeEndretEventData getOrgtilknytningTypeEndretEventData() {
        return (OrgtilknytningTypeEndretEventData) getData();
    }

    @Builder(toBuilder = true)
    public record OrgtilknytningTypeEndretEventData(
        Optional<OrgtilknytningType> orgtilknytningType
    ) implements Data {}
}
