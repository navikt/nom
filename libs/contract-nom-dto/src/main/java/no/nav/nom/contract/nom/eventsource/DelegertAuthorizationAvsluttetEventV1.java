package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class DelegertAuthorizationAvsluttetEventV1 extends AuthorizationEvent {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DelegertAuthorizationAvsluttetEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") DelegertAuthorizationAvsluttetEventData data,
        @JsonProperty("metadata") Metadata metadata
    ) {
        super(fid, data, metadata);
    }

    @JsonIgnore
    public DelegertAuthorizationAvsluttetEventV1(String fid, DelegertAuthorizationAvsluttetEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record DelegertAuthorizationAvsluttetEventData(
        String til,
        String av,
        String forOrgenhet,
        LocalDate gyldigFom,
        LocalDate opprinneligGyldigTom,
        LocalDate nyGyldigTom
    ) implements Data {}
}
