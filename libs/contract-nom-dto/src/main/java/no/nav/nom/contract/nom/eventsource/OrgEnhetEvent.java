package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
public abstract class OrgEnhetEvent extends Event {

    public OrgEnhetEvent(String fid, Data data, Metadata metadata, OrgEnhetEvent.Data additionalData) {
        super(fid, data, metadata, additionalData);
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type"
    )
    public interface Data extends Event.Data { }
}
