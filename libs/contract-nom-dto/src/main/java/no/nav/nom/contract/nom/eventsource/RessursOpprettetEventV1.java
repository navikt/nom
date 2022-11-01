package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class RessursOpprettetEventV1 extends Event {

    @JsonCreator
    public RessursOpprettetEventV1(UUID fid, RessursRegisteredEventData data, Metadata metadata){
        super(fid, data, metadata);
    }

    public RessursOpprettetEventV1(RessursRegisteredEventData data, InputMetadata inputMetadata) {
        super(UUID.randomUUID(), data, new Metadata(inputMetadata, LocalDateTime.now()));
    }

    public record RessursRegisteredEventData(
            String personident,
            String navident,
            Sektor sektor,
            Visningsnavn visningsnavn
    ) implements Data{

        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                    "personident", personident,
                    "navident", navident,
                    "sektor", sektor,
                    "visningsnavn", visningsnavn
            );
        }

        public record Visningsnavn(
                String fornavn,
                String mellomnavn,
                String etternavn
        ){}

        public enum Sektor{
            NAV_STATLIG,
            NAV_KOMMUNAL,
            EKSTERN
        }
    }
}