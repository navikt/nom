package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrgtilknytningEvent.class),
    @JsonSubTypes.Type(value = RessursEvent.class)
})
public abstract class Event {
    private final String fid;
    private final Data data;
    private final Metadata metadata;

    private final Data additionalData;

    public Event(String fid, Data data, Metadata metadata, Data additionalData) {
        this.fid = fid;
        this.data = data;
        this.metadata = metadata;
        this.additionalData = additionalData;
    }

    public String getFid() {
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
            property = "type"
    )
    public interface Data{ }

    @Override
    public String toString() {
        return "Event{" +
                "fid='" + fid + '\'' +
                ", data=" + data +
                ", metadata=" + metadata +
                ", additionalData=" + additionalData +
                '}';
    }
}
