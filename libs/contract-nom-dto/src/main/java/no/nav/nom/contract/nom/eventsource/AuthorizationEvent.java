package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
    @Type(value = DelegertAuthorizationOpprettetEventV1.class),
    @Type(value = DelegertAuthorizationFornyetEventV1.class),
    @Type(value = DelegertAuthorizationAvsluttetEventV1.class)
})
public abstract class AuthorizationEvent extends Event {

    public AuthorizationEvent(String fid, Data data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type"
    )
    public interface Data extends Event.Data { }
}
