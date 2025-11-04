package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import no.nav.nom.contract.nom.Tilknytningtype;

public class OrgtilknytningTilknytningtypeEndretEventV1 extends OrgtilknytningEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgtilknytningTilknytningtypeEndretEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgtilknytningTilknytningtypeEndretEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
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
