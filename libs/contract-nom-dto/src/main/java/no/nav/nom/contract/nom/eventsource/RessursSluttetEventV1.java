package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RessursSluttetEventV1 extends RessursEvent {

    @JsonCreator
    public RessursSluttetEventV1(String fid, RessursSluttetEventData data, Metadata metadata){
        super(fid, data, metadata, null);
    }

    public RessursSluttetEventV1(String fid, RessursSluttetEventData data, InputMetadata inputMetadata) {
        super(fid, data, new Metadata(inputMetadata, LocalDateTime.now()), null);
    }

    @JsonIgnore
    public RessursSluttetEventData getRessursSluttetEventData() {
        return (RessursSluttetEventData) getData();
    }

    public record RessursSluttetEventData(
            LocalDate sluttdato
    ) implements Data{
    }
}