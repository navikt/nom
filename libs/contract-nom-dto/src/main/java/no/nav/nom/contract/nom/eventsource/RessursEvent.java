package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @Type(value = RessursOpprettetEventV1.class),
        @Type(value = RessursSluttetEventV1.class)
})
public abstract class RessursEvent extends Event {

    public RessursEvent(String fid, Data data, Metadata metadata, Data additionalData) {
        super(fid, data, metadata, additionalData);
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type")
    @JsonSubTypes({
            @Type(value = RessursOpprettetEventV1.RessursOpprettetEventData.class),
            @Type(value = RessursOpprettetEventV1.RessursOpprettetAdditionalData.class),
            @Type(value = RessursSluttetEventV1.RessursSluttetEventData.class)
    })
    public interface Data extends Event.Data { }
}
