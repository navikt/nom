package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes(
        @JsonSubTypes.Type(value = RessursOpprettetEventV1.class)
)
@AllArgsConstructor
public abstract class Event {
    private final UUID fid;
    private final Data data;
    private final Metadata metadata;

    public UUID getFid() {
        return fid;
    }

    public Data getData() {
        return data;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public record Metadata(
        InputMetadata inputMetadata,
        LocalDateTime createdTimestamp
    ){}

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes(
            @JsonSubTypes.Type(value = RessursOpprettetEventV1.RessursRegisteredEventData.class)
    )
    public interface Data{
        public Map<String, Object> toMap();
        default public Optional<?> getAsOptional(String property){
          return Optional.ofNullable(toMap().get(property));
        }
    }
}
