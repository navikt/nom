package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.UUID;

public class RessursOpprettetEventV1 extends Event {

    @JsonCreator
    public RessursOpprettetEventV1(UUID fid, RessursRegisteredEventData data, Metadata metadata){
        super(fid, data, metadata);
    }

    public RessursOpprettetEventV1(UUID fid, RessursRegisteredEventData data, InputMetadata inputMetadata) {
        this(fid, data, Metadata.from(inputMetadata));
    }

    public RessursOpprettetEventV1(RessursRegisteredEventData data, InputMetadata inputMetadata) {
        this(UUID.randomUUID(), data, Metadata.from(inputMetadata));
    }

    public record RessursRegisteredEventData(
            String personident,
            String navident,
            Sektor sektor,
            Visningsnavn visningsnavn
    ) implements Data{
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

    public record RessursOpprettetAdditionalData(
            String navident,
            String kjoenn,
            FolkeregistrertNavn visningsnavn
    ) implements Data{
        public record FolkeregistrertNavn(
                String fornavn,
                String mellomnavn,
                String etternavn
        ){}
    }
}