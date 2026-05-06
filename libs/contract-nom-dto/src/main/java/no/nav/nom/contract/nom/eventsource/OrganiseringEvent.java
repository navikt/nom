package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrganiseringOpprettetEventV1.class),
    @JsonSubTypes.Type(value = OrganiseringEndretEventV1.class),
    @JsonSubTypes.Type(value = OrganiseringEndretNavnEventV1.class),
    @JsonSubTypes.Type(value = OrganiseringAvsluttetEventV1.class)
})

public abstract class OrganiseringEvent extends Event {

    public OrganiseringEvent(String fid, Data data, Metadata metadata, OrganiseringEvent.Data additionalData) {
        super(fid, data, metadata, additionalData);
    }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
    )
    public interface Data extends Event.Data {}
}
