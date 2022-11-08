package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class RessursSluttetEventV1 extends Event {

    @JsonCreator
    public RessursSluttetEventV1(UUID fid, RessursSluttetEventData data, Metadata metadata){
        super(fid, data, metadata);
    }

    public RessursSluttetEventV1(UUID fid, RessursSluttetEventData data, InputMetadata inputMetadata) {
        super(fid, data, new Metadata(inputMetadata, LocalDateTime.now()));
    }

    public RessursSluttetEventV1(RessursSluttetEventData data, InputMetadata inputMetadata) {
        super(UUID.randomUUID(), data, new Metadata(inputMetadata, LocalDateTime.now()));
    }

    public record RessursSluttetEventData(
            LocalDate sluttdato
    ) implements Data{

        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                    "sluttdato", sluttdato
            );
        }

        public record Visningsnavn(
                String fornavn,
                String mellomnavn,
                String etternavn
        ){}
    }
}