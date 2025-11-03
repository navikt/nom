package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDateTime;

public class RessursTilknytningtypeOppdatertV1 extends RessursEvent {
    @JsonCreator
    public RessursTilknytningtypeOppdatertV1(String fid, TilknytningtypeOppdatertEventData data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public RessursTilknytningtypeOppdatertV1(String fid, TilknytningtypeOppdatertEventData data, InputMetadata inputMetadata) {
        super(fid, data, new Metadata(inputMetadata, LocalDateTime.now()), null);
    }

    public record TilknytningtypeOppdatertEventData(
        Tilknytningtype sektor
    ) implements Data{ }
}
