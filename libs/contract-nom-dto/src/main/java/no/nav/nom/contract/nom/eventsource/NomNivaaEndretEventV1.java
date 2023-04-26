package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.nom.contract.nom.OrgEnhetsKategori;

import java.util.Optional;

public class NomNivaaEndretEventV1 extends OrgEnhetEvent {

    @JsonCreator
    public NomNivaaEndretEventV1(String fid, NomNivaaEndretEventData data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public NomNivaaEndretEventV1(String fid, NomNivaaEndretEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record NomNivaaEndretEventData(
        Optional<OrgEnhetsKategori> nomNivaa
    ) implements Data {}

}