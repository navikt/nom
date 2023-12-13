package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.util.List;

public class RessursFysiskeBrukerstederOppdatertEventV1 extends RessursEvent {

    @JsonCreator
    public RessursFysiskeBrukerstederOppdatertEventV1(String fid, RessursFysiskeBrukerstederOppdatertEventData data, Metadata metadata){
        super(fid, data, metadata, null);
    }

    public RessursFysiskeBrukerstederOppdatertEventV1(String fid, RessursFysiskeBrukerstederOppdatertEventData data, InputMetadata inputMetadata){
        this(fid, data, Metadata.from(inputMetadata));
    }

    @JsonIgnore
    public RessursFysiskeBrukerstederOppdatertEventData getRessursFysiskeBrukerstederOppdatertEventData() {
        return (RessursFysiskeBrukerstederOppdatertEventData) getData();
    }

    @Builder(toBuilder = true)
    public record RessursFysiskeBrukerstederOppdatertEventData(
        List<String> fysiskeBrukersteder,
        String primaryFysiskBrukersted
    ) implements Data {}
}
