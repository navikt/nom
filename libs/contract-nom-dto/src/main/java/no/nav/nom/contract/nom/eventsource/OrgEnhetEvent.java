package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
})
public abstract class OrgEnhetEvent extends Event {

    public OrgEnhetEvent(String fid, Data data, Metadata metadata, Data additionalData) {
        super(fid, data, metadata, additionalData);
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type")
    @JsonSubTypes({
    })
    public interface Data extends Event.Data { }
}
