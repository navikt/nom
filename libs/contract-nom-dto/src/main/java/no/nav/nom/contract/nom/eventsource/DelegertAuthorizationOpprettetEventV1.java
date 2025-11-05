package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class DelegertAuthorizationOpprettetEventV1 extends AuthorizationEvent {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DelegertAuthorizationOpprettetEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") DelegertAuthorizationOpprettetEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata);
    }

    @JsonIgnore
    public DelegertAuthorizationOpprettetEventV1(String fid, DelegertAuthorizationOpprettetEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record DelegertAuthorizationOpprettetEventData(
        String til,
        String av,
        String forOrgenhet,
        LocalDate gyldigFom,
        LocalDate gyldigTom
    ) implements Data {}
}
