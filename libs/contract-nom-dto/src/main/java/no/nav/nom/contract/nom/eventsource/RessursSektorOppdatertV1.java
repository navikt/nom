package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDateTime;

public class RessursSektorOppdatertV1 extends RessursEvent{
    @JsonCreator
    public RessursSektorOppdatertV1(String fid, SektorOppdatertEventData data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public RessursSektorOppdatertV1(String fid, SektorOppdatertEventData data, InputMetadata inputMetadata) {
        super(fid, data, new Metadata(inputMetadata, LocalDateTime.now()), null);
    }

    public record SektorOppdatertEventData(
        Sektor sektor
    ) implements Data{ }
}
