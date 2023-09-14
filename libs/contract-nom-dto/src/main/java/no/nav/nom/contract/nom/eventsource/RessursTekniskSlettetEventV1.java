package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDateTime;

public class RessursTekniskSlettetEventV1 extends RessursEvent {

    @JsonCreator
    public RessursTekniskSlettetEventV1(String fid, RessursTekniskSlettetEventData data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public RessursTekniskSlettetEventV1(String fid, RessursTekniskSlettetEventData data, InputMetadata inputMetadata) {
        super(fid, data, new Metadata(inputMetadata, LocalDateTime.now()), null);
    }

    public record RessursTekniskSlettetEventData() implements Data {}
}