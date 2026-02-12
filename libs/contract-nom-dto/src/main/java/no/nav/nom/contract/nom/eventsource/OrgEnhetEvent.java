package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
    @Type(value = OrgEnhetOpprettetEventV1.class),
    @Type(value = OrgEnhetAvvikletEventV1.class),
    @Type(value = OrgEnhetKorrigertEventV1.class),
    @Type(value = OrgEnhetTypeEndretEventV1.class),
    @Type(value = NomNivaaEndretEventV1.class),
    @Type(value = OrgEnhetEndretNavnEventV1.class)
})
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
