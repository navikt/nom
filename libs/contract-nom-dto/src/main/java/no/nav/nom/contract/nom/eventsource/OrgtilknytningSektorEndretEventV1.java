package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;

public class OrgtilknytningSektorEndretEventV1 extends OrgtilknytningEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgtilknytningSektorEndretEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgtilknytningSektorEndretEventData data,
        @JsonProperty("metadata") Metadata metadata,
        @JsonProperty("additionalData") AdditionalAgressoData additionalData
    ) {
        super(fid, data, metadata, additionalData);
    }

    @JsonIgnore
    public OrgtilknytningSektorEndretEventV1(
        String fid,
        OrgtilknytningSektorEndretEventData data,
        InputMetadata inputMetadata,
        OrgtilknytningEvent.AdditionalAgressoData additionalData
    ) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    @Builder(toBuilder = true)
    public record OrgtilknytningSektorEndretEventData(
        String navident,
        String orgEnhetId,
        Sektor sektor
    ) implements Data {}
}
