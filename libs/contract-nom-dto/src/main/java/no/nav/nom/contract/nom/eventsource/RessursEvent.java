package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = RessursSluttdatoOppdatertEventV1.class),
    @JsonSubTypes.Type(value = RessursSluttetEventV1.class)
})
public abstract class RessursEvent extends Event {

    public RessursEvent(String fid, Data data, Metadata metadata, Data additionalData) {
        super(fid, data, metadata, additionalData);
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type"
    )
    public interface Data extends Event.Data { }
}
