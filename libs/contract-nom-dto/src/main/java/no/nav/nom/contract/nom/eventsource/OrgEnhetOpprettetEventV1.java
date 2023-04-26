package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.nom.contract.nom.OrgEnhetsKategori;

import java.time.LocalDate;
import java.util.Optional;

public class OrgEnhetOpprettetEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public OrgEnhetOpprettetEventV1(String fid, OrgEnhetOpprettetEventData data, Metadata metadata, OrgEnhetOpprettetAdditionalAgressoData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public OrgEnhetOpprettetEventV1(String fid, OrgEnhetOpprettetEventData data, InputMetadata inputMetadata, OrgEnhetOpprettetAdditionalAgressoData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public record OrgEnhetOpprettetEventData(
            String id,
            String navn,
            LocalDate gyldigFom,
            Optional<String> overordnetOrgEnhetFid,
            Optional<String> lederRessursFid
    ) implements Data{ }

    public record OrgEnhetOpprettetAdditionalAgressoData(
            String agressoId,
            String agressoNavn,
            String agressoOrgNivaa,
            Optional<String> agressoOrgenhetType,
            String uniqueAgressoId
    ) implements OrgEnhetEvent.Data {
    }
}