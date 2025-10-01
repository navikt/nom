package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;

public class OrgEnhetOpprettetEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public OrgEnhetOpprettetEventV1(
            String fid,
            OrgEnhetOpprettetEventData data,
            Event.Metadata metadata,
            OrgEnhetOpprettetAdditionalAgressoData additionalData
    ) {
        super(fid, data, metadata, additionalData);
    }

    public OrgEnhetOpprettetEventV1(String fid, OrgEnhetOpprettetEventData data, InputMetadata inputMetadata, OrgEnhetOpprettetAdditionalAgressoData additionalData) {
        this(fid, data, Event.Metadata.from(inputMetadata), additionalData);
    }

    public record OrgEnhetOpprettetEventData(
            String id,
            String navn,
            LocalDate gyldigFom,
            String overordnetOrgEnhetFid,
            String lederRessursFid
    ) implements Data{ }

    public record OrgEnhetOpprettetAdditionalAgressoData(
            String agressoId,
            String agressoNavn,
            String agressoOrgNivaa,
            String agressoOrgenhetType,
            String uniqueAgressoId
    ) implements OrgEnhetEvent.Data {
    }
}