package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

public class RessursJobbtelefonOppdatertEventV1 extends RessursEvent {

    @JsonCreator
    public RessursJobbtelefonOppdatertEventV1(String fid, JobbtelefonOppdatertEventData data, Metadata metadata) {
        super(fid, data, metadata, null);
    }

    public RessursJobbtelefonOppdatertEventV1(String fid, JobbtelefonOppdatertEventData data, InputMetadata inputMetadata) {
        super(fid, data, new Metadata(inputMetadata, LocalDateTime.now()), null);
    }

    @JsonIgnore
    public RessursJobbtelefonOppdatertEventV1.JobbtelefonOppdatertEventData getJobbtelefonOppdatertEventData() {
        return (JobbtelefonOppdatertEventData) getData();
    }

    public record JobbtelefonOppdatertEventData(
        String navident,
        Jobbtelefon jobbtelefon
    ) implements Data {}
}
