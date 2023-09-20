package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RessursStartDatoOppdatertV1 extends RessursEvent {

    @JsonCreator
    public RessursStartDatoOppdatertV1(String fid, StartDatoOppdatertEventData data, Metadata metadata){
        super(fid, data, metadata,null);
    }

    public RessursStartDatoOppdatertV1(String fid, StartDatoOppdatertEventData data, InputMetadata inputMetadata){
        super(fid, data, new Metadata(inputMetadata, LocalDateTime.now()), null);
    }

    public record StartDatoOppdatertEventData(
        LocalDate startdato
    ) implements Data{ }
}
