package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes(
    {
        @Type(value = OrgtilknytningAvsluttetEventV1.class),
        @Type(value = OrgtilknytningDagligOppfolgingEndretEventV1.class),
        @Type(value = OrgtilknytningDatoEndretEventV1.class),
        @Type(value = OrgtilknytningDatoEndretEventV2.class),
        @Type(value = OrgtilknytningFjernetEventV1.class),
        @Type(value = OrgtilknytningOpprettetEventV1.class),
        @Type(value = OrgtilknytningSektorEndretEventV1.class),
        @Type(value = OrgtilknytningTilknytningtypeEndretEventV1.class),
        @Type(value = OrgtilknytningTypeEndretEventV1.class)
    }
)
public abstract class OrgtilknytningEvent extends Event {

    public OrgtilknytningEvent(String fid, Data data, Metadata metadata, OrgtilknytningEvent.Data additionalData) {
        super(fid, data, metadata, additionalData);
    }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
    )
    public interface Data extends Event.Data {}

    public record AdditionalAgressoData(
        String hrresStillingRessursEnhet,
        String hrresStillingerPerioderFormatert
    ) implements OrgtilknytningEvent.Data {
    }
}
