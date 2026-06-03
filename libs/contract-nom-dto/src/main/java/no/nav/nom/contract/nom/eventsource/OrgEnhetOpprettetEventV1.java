package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class OrgEnhetOpprettetEventV1 extends OrgEnhetEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgEnhetOpprettetEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgEnhetOpprettetEventData data,
        @JsonProperty("metadata") Metadata metadata,
        @JsonProperty("additionalData") OrgEnhetOpprettetAdditionalAgressoData additionalData
    ) {
        super(fid, data, metadata, additionalData);
    }

    public OrgEnhetOpprettetEventV1(
        String fid,
        OrgEnhetOpprettetEventData data,
        InputMetadata inputMetadata,
        OrgEnhetOpprettetAdditionalAgressoData additionalData
    ) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public record OrgEnhetOpprettetEventData(
        String id,
        String navn,
        LocalDate gyldigFom,
        String overordnetOrgEnhetFid,
        String lederRessursFid,
        String tilgangsenhetId
    ) implements Data {}

    public record OrgEnhetOpprettetAdditionalAgressoData(
        String agressoId,
        String agressoNavn,
        String agressoOrgNivaa,
        String agressoOrgenhetType,
        String uniqueAgressoId
    ) implements OrgEnhetEvent.Data {
    }
}