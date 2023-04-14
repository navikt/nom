package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.util.Optional;

public class OrgtilknytningDatoEndretEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public OrgtilknytningDatoEndretEventV1(String fid, OrgtilknytningDatoEndretEventData data, Metadata metadata, OrgtilknytningDatoEndretAdditionalAgressoData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public OrgtilknytningDatoEndretEventV1(String fid, OrgtilknytningDatoEndretEventData data, InputMetadata inputMetadata, OrgtilknytningDatoEndretAdditionalAgressoData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public record OrgtilknytningDatoEndretEventData(
            String navident,
            String orgEnhetId,
            LocalDate gyldigFom,
            Optional<LocalDate> gyldigTom
    ) implements Data{ }

    public record OrgtilknytningDatoEndretAdditionalAgressoData(
        String hrresStillingRessursEnhet,
        String hrresStillingerPerioderFormatert
    ) implements Data { }
}