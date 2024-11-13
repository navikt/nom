package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;

public class OrgtilknytningSektorEndretEventV1 extends OrgtilknytningEvent {

    @JsonCreator
    public OrgtilknytningSektorEndretEventV1(String fid, OrgtilknytningSektorEndretEventData data, Metadata metadata, OrgtilknytningEvent.AdditionalAgressoData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public OrgtilknytningSektorEndretEventV1(String fid, OrgtilknytningSektorEndretEventData data, InputMetadata inputMetadata, OrgtilknytningEvent.AdditionalAgressoData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    @JsonIgnore
    public OrgtilknytningSektorEndretEventData getOrgtilknytningSektorEndretEventData() {
        return (OrgtilknytningSektorEndretEventData) getData();
    }

    @Builder(toBuilder = true)
    public record OrgtilknytningSektorEndretEventData(
            String navident,
            String orgEnhetId,
            Sektor sektor
    ) implements Data{ }
}
