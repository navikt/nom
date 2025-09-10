package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import no.nav.nom.contract.nom.Tilknytningtype;

public class OrgtilknytningTilknytningtypeEndretEventV1 extends OrgtilknytningEvent {

    @JsonCreator
    public OrgtilknytningTilknytningtypeEndretEventV1(String fid, OrgtilknytningTilknytningtypeEndretEventData data, Metadata metadata){
        super(fid, data, metadata, null);
    }

    public OrgtilknytningTilknytningtypeEndretEventV1(String fid, OrgtilknytningTilknytningtypeEndretEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    @JsonIgnore
    public OrgtilknytningTilknytningtypeEndretEventData getOrgtilknytningTilknytningtypeEndretEventData() {
        return (OrgtilknytningTilknytningtypeEndretEventData) getData();
    }

    @Builder(toBuilder = true)
    public record OrgtilknytningTilknytningtypeEndretEventData(
        Tilknytningtype orgtilknytningType,
        String masseflyttetFraFid
    ) implements Data {
        public OrgtilknytningTilknytningtypeEndretEventData(Tilknytningtype orgtilknytningType) {
            this(orgtilknytningType, null);
        }
    }
}
