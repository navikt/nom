package no.nav.nom.contract.nom.eventsource.orgenhetleder;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.nom.contract.nom.eventsource.Event;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes({
    @Type(value = OrgEnhetLederOpprettetEventV1.class),
    @Type(value = OrgEnhetLederStartdatoOppdatertEventV1.class),
    @Type(value = OrgEnhetLederSluttdatoOppdatertEventV1.class),
    @Type(value = OrgEnhetLederSlettetEventV1.class)
})
public abstract class OrgEnhetLederEvent extends Event {

    public OrgEnhetLederEvent(String fid, Data data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
    )
    public interface Data extends Event.Data {}
}
