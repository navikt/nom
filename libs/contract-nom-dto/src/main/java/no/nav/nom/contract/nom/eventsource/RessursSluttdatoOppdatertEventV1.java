package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public class RessursSluttdatoOppdatertEventV1 extends RessursEvent {

    @JsonCreator
    public RessursSluttdatoOppdatertEventV1(String fid, RessursSluttdatoOppdatertEventV1.RessursSluttdatoOppdatertEventData data, Metadata metadata){
        super(fid, data, metadata, null);
    }

    public RessursSluttdatoOppdatertEventV1(String fid, RessursSluttdatoOppdatertEventV1.RessursSluttdatoOppdatertEventData data, InputMetadata inputMetadata) {
        super(fid, data, new Metadata(inputMetadata, LocalDateTime.now()), null);
    }

    @JsonIgnore
    public RessursSluttdatoOppdatertEventV1.RessursSluttdatoOppdatertEventData getRessursSluttdatoOppdatertEventData() {
        return (RessursSluttdatoOppdatertEventV1.RessursSluttdatoOppdatertEventData) getData();
    }

    public record RessursSluttdatoOppdatertEventData(
        Optional<LocalDate> sluttdato
    ) implements Data{
    }
}
