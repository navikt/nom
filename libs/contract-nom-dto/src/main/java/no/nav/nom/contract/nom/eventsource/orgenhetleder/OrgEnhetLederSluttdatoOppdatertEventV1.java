package no.nav.nom.contract.nom.eventsource.orgenhetleder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.nom.contract.nom.eventsource.InputMetadata;

import java.time.LocalDate;

public class OrgEnhetLederSluttdatoOppdatertEventV1 extends OrgEnhetLederEvent {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrgEnhetLederSluttdatoOppdatertEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") OrgEnhetLederAvsluttetEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata);
    }

    @JsonIgnore
    public OrgEnhetLederSluttdatoOppdatertEventV1(String fid, OrgEnhetLederAvsluttetEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record OrgEnhetLederAvsluttetEventData(
        String orgEnhetId,
        LocalDate sluttdato
    ) implements Data {}
}
