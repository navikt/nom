package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class DelegertAuthorizationFornyetEventV1 extends AuthorizationEvent {
    @JsonCreator
    public DelegertAuthorizationFornyetEventV1(@JsonProperty("fid") String fid,
                                               @JsonProperty("data") DelegertAuthorizationFornyetEventData data,
                                               @JsonProperty("metadata") Metadata metadata) {
        super(fid, data, metadata);
    }

    public DelegertAuthorizationFornyetEventV1(String fid, DelegertAuthorizationFornyetEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public record DelegertAuthorizationFornyetEventData (
        String til,
        String av,
        String forOrgenhet,
        LocalDate gyldigFom,
        LocalDate opprinneligGyldigTom,
        LocalDate nyGyldigTom
    ) implements Data {}
}
