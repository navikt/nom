package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.nom.contract.nom.OrgEnhetsKategori;

import java.time.LocalDate;
import java.util.Optional;

public class OrgEnhetOpprettetEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public OrgEnhetOpprettetEventV1(String fid, OrgEnhetOpprettetEventData data, Metadata metadata){
        super(fid, data, metadata, null);
    }

    public OrgEnhetOpprettetEventV1(String fid, OrgEnhetOpprettetEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record OrgEnhetOpprettetEventData(
            String id,
            String navn,
            LocalDate gyldigFom,
            Optional<String> overordnetOrgEnhetFid,
            Optional<String> lederRessursFid,
            OrgEnhetsKategori orgEnhetsKategori
    ) implements Data{ }
}