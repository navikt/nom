package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.nom.contract.nom.OrgEnhetsKategori;

import java.time.LocalDate;
import java.util.Optional;

public class OrgEnhetKorrigertEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public OrgEnhetKorrigertEventV1(String fid, OrgEnhetKorrigertEventData data, Metadata metadata, OrgEnhetKorrigertAdditionalAgressoData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public OrgEnhetKorrigertEventV1(String fid, OrgEnhetKorrigertEventData data, InputMetadata inputMetadata, OrgEnhetKorrigertAdditionalAgressoData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public record OrgEnhetKorrigertEventData(
            String id,
            String navn,
            LocalDate gyldigFom,
            Optional<LocalDate> gyldigTom,
            Optional<String> overordnetOrgEnhetFid,
            Optional<String> lederRessursFid,
            OrgEnhetsKategori orgEnhetsKategori
    ) implements Data{ }

    public record OrgEnhetKorrigertAdditionalAgressoData(
            String agressoId,
            String agressoNavn,
            String agressoOrgNivaa,
            Optional<String> agressoOrgenhetType,
            String uniqueAgressoId
    ) implements OrgEnhetEvent.Data { }
}