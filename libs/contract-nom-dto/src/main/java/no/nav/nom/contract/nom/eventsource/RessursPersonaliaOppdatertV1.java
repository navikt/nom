package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDateTime;

public class RessursPersonaliaOppdatertV1 extends RessursEvent{
    @JsonCreator
    public RessursPersonaliaOppdatertV1(String fid, PersonaliaOppdatertEventData data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public RessursPersonaliaOppdatertV1(String fid, PersonaliaOppdatertEventData data, InputMetadata inputMetadata) {
        super(fid, data, new Metadata(inputMetadata, LocalDateTime.now()), null);
    }

    public record PersonaliaOppdatertEventData(
        Visningsnavn visningsnavn
    ) implements Data{ }
}
