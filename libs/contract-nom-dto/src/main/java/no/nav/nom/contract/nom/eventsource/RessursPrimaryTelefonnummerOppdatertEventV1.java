package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

public class RessursPrimaryTelefonnummerOppdatertEventV1 extends RessursEvent {


    @JsonCreator
    public RessursPrimaryTelefonnummerOppdatertEventV1(String fid, PrimaryTelefonnummerOppdatertEventData data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public RessursPrimaryTelefonnummerOppdatertEventV1(String fid, PrimaryTelefonnummerOppdatertEventData data, InputMetadata inputMetadata) {
        super(fid, data, new Metadata(inputMetadata, LocalDateTime.now()), null);
    }

    @JsonIgnore
    public PrimaryTelefonnummerOppdatertEventData getMobilnummerOppdatertEventData() {
        return (PrimaryTelefonnummerOppdatertEventData) getData();
    }

    public record PrimaryTelefonnummerOppdatertEventData(
        String primaryTelefonnummer
    ) implements Data{ }
}
