package no.nav.nom.contract.nom.eventsource.orgenhetleder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;

public class OrgEnhetLederStartdatoOppdatertEventV1 extends OrgEnhetLederEvent {
    @JsonCreator
    public OrgEnhetLederStartdatoOppdatertEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgEnhetLederStartdatoOppdatertData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata);
    }

    public OrgEnhetLederStartdatoOppdatertEventV1(String fid, OrgEnhetLederStartdatoOppdatertData data, InputMetadata metadata) {
        this(fid, data, Metadata.from(metadata));
    }

    public record OrgEnhetLederStartdatoOppdatertData(
        LocalDate startdato
    ) implements Data {}
}
