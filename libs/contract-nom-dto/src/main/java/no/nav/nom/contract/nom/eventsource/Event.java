package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes(
        @Type(value = RessursOpprettetEventV1.class)
)
public abstract class Event {
    private final UUID fid;
    private final Data data;
    private final Metadata metadata;

    private Data additionalData;

    public Event(UUID fid, Data data, Metadata metadata) {
        this.fid = fid;
        this.data = data;
        this.metadata = metadata;
    }

    public UUID getFid() {
        return fid;
    }

    public Data getData() {
        return data;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Data getAdditionalData() {
        return additionalData;
    }

    public record Metadata(
        InputMetadata inputMetadata,
        LocalDateTime createdTimestamp
    ){
        public static Metadata from(InputMetadata metadata){
            return new Metadata(metadata, LocalDateTime.now());
        }
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type")
    @JsonSubTypes({
            @Type(value = RessursOpprettetEventV1.RessursRegisteredEventData.class),
            @Type(value = RessursOpprettetEventV1.RessursOpprettetAdditionalData.class)
    })
    public interface Data{ }
}
