package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.util.Optional;

public class OrgtilknytningPrimaerEndretEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public OrgtilknytningPrimaerEndretEventV1(String fid, OrgtilknytningPrimaerEndretEventData data, Metadata metadata, OrgtilknytningPrimaerEndretAdditionalAgressoData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public OrgtilknytningPrimaerEndretEventV1(String fid, OrgtilknytningPrimaerEndretEventData data, InputMetadata inputMetadata, OrgtilknytningPrimaerEndretAdditionalAgressoData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public record OrgtilknytningPrimaerEndretEventData(
            String navident,
            String orgEnhetId,
            boolean primaer,
            LocalDate gyldigFom,
            Optional<LocalDate> gyldigTom
    ) implements Data{ }

    public record OrgtilknytningPrimaerEndretAdditionalAgressoData(
        String hrresStillingRessursEnhet,
        String hrresStillingerPerioderFormatert
    ) implements Data { }
}