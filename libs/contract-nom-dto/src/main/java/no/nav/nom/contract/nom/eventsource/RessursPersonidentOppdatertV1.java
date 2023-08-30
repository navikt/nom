package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDateTime;

public class RessursPersonidentOppdatertV1 extends RessursEvent {
    @JsonCreator
    public RessursPersonidentOppdatertV1(String fid, PersonidentOppdatertEventData data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public RessursPersonidentOppdatertV1(String fid, PersonidentOppdatertEventData data, InputMetadata inputMetadata) {
        super(fid, data, new Metadata(inputMetadata, LocalDateTime.now()), null);
    }

    public record PersonidentOppdatertEventData(
        String personident
    ) implements Data{ }
}
