package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = RessursFysiskeBrukerstederOppdatertEventV1.class),
        @JsonSubTypes.Type(value = RessursJobbtelefonOppdatertEventV1.class),
        @JsonSubTypes.Type(value = RessursOpprettetEventV1.class),
        @JsonSubTypes.Type(value = RessursPersonaliaOppdatertV1.class),
        @JsonSubTypes.Type(value = RessursPersonidentOppdatertV1.class),
        @JsonSubTypes.Type(value = RessursPrimaryTelefonnummerOppdatertEventV1.class),
        @JsonSubTypes.Type(value = RessursSektorOppdatertV1.class),
        @JsonSubTypes.Type(value = RessursSluttdatoOppdatertEventV1.class),
        @JsonSubTypes.Type(value = RessursSluttetEventV1.class),
        @JsonSubTypes.Type(value = RessursStartDatoOppdatertV1.class),
        @JsonSubTypes.Type(value = RessursTekniskSlettetEventV1.class),
        @JsonSubTypes.Type(value = RessursTilknytningtypeOppdatertV1.class),
    }
)
public abstract class RessursEvent extends Event {

    public RessursEvent(String fid, Data data, Metadata metadata, Data additionalData) {
        super(fid, data, metadata, additionalData);
    }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
    )
    public interface Data extends Event.Data {}
}
