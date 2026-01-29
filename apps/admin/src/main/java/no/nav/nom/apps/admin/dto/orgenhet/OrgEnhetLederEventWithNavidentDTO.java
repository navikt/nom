package no.nav.nom.apps.admin.dto.orgenhet;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederEvent;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederSlettetEventV1;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederSluttdatoOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederStartdatoOppdatertEventV1;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrgEnhetLederOpprettetEventV1.class),
    @JsonSubTypes.Type(value = OrgEnhetLederStartdatoOppdatertEventV1.class),
    @JsonSubTypes.Type(value = OrgEnhetLederSluttdatoOppdatertEventV1.class),
    @JsonSubTypes.Type(value = OrgEnhetLederSlettetEventV1.class)
})
public class OrgEnhetLederEventWithNavidentDTO extends OrgEnhetLederEvent {
    private final String navident;

    public OrgEnhetLederEventWithNavidentDTO(String fid, Data data, Metadata metadata, String navident) {
        super(fid, data, metadata);
        this.navident = navident;
    }

    public String getNavident() {
        return navident;
    }
}
