package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class OrgEnhetKorrigertEventV1 extends OrgEnhetEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgEnhetKorrigertEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgEnhetKorrigertEventData data,
        @JsonProperty("metadata") Metadata metadata,
        @JsonProperty("additionalData") OrgEnhetKorrigertAdditionalAgressoData additionalData
    ) {
        super(fid, data, metadata, additionalData);
    }

    @JsonIgnore
    public OrgEnhetKorrigertEventV1(
        String fid,
        OrgEnhetKorrigertEventData data,
        InputMetadata inputMetadata,
        OrgEnhetKorrigertAdditionalAgressoData additionalData
    ) {
        this(fid, data, Event.Metadata.from(inputMetadata), additionalData);
    }

    public record OrgEnhetKorrigertEventData(
        String id,
        String navn,
        LocalDate gyldigFom,
        LocalDate gyldigTom,
        String overordnetOrgEnhetFid,
        String lederRessursFid
    ) implements Data {}

    public record OrgEnhetKorrigertAdditionalAgressoData(
        String agressoId,
        String agressoNavn,
        String agressoOrgNivaa,
        String agressoOrgenhetType,
        String uniqueAgressoId
    ) implements OrgEnhetEvent.Data {}
}