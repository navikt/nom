package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;

public class OrgtilknytningOpprettetEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public OrgtilknytningOpprettetEventV1(String fid, OrgtilknytningOpprettetEventData data, Metadata metadata, OrgtilknytningOpprettetAdditionalAgressoData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public OrgtilknytningOpprettetEventV1(String fid, OrgtilknytningOpprettetEventData data, InputMetadata inputMetadata, OrgtilknytningOpprettetAdditionalAgressoData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public record OrgtilknytningOpprettetEventData(
            String navident,
            String orgEnhetId,
            boolean erPrimaer,
            LocalDate gyldigFom
    ) implements Data{ }

    public record OrgtilknytningOpprettetAdditionalAgressoData(
            String hrresStillingRessursEnhet,
            String hrresStillingerPerioderFormatert
    ) implements Data {
    }
}