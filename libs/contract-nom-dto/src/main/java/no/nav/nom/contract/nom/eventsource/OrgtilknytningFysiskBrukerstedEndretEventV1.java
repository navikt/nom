package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

public class OrgtilknytningFysiskBrukerstedEndretEventV1 extends OrgtilknytningEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgtilknytningFysiskBrukerstedEndretEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgtilknytningPrimaryFysiskBrukerstedEventData data,
        @JsonProperty("metadata") Metadata metadata,
        @JsonProperty("additionalData") AdditionalAgressoData additionalData
    ) {
        super(fid, data, metadata, additionalData);
    }

    @JsonIgnore
    public OrgtilknytningFysiskBrukerstedEndretEventV1(
        String fid,
        OrgtilknytningPrimaryFysiskBrukerstedEventData data,
        InputMetadata inputMetadata
    ) {
        this(fid, data, Metadata.from(inputMetadata), null);
    }

    @Builder(toBuilder = true)
    public record OrgtilknytningPrimaryFysiskBrukerstedEventData(
        String navident,
        String orgEnhetId,
        String primaryFysiskBrukerstedNummer,
        List<String> additionalFysiskeBrukerstederNummer
    ) implements Data {}
}
