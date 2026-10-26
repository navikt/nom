package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class KorrigerGyldigFomEventV1 extends OrgEnhetEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public KorrigerGyldigFomEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") KorrigerGyldigFomEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata, null);
    }

    @JsonIgnore
    public KorrigerGyldigFomEventV1(String fid, KorrigerGyldigFomEventData data, InputMetadata metadata) {
        this(fid, data, Metadata.from(metadata));
    }

    public record KorrigerGyldigFomEventData(
        String id,
        LocalDate gyldigFom
    ) implements Data {}
}
