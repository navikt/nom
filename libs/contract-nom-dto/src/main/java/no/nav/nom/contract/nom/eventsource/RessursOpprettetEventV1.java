package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Optional;
import java.util.UUID;

public class RessursOpprettetEventV1 extends Event {

    @JsonCreator
    public RessursOpprettetEventV1(UUID fid, RessursRegisteredEventData data, Metadata metadata, RessursOpprettetAdditionalData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public RessursOpprettetEventV1(UUID fid, RessursRegisteredEventData data, InputMetadata inputMetadata, RessursOpprettetAdditionalData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public RessursOpprettetEventV1(RessursRegisteredEventData data, InputMetadata inputMetadata, RessursOpprettetAdditionalData additionalData) {
        this(UUID.randomUUID(), data, Metadata.from(inputMetadata), additionalData);
    }

    public record RessursRegisteredEventData(
            String personident,
            Sektor sektor,
            Visningsnavn visningsnavn
    ) implements Data{ }

    public record RessursOpprettetAdditionalData(
            Optional<String> navident,
            Optional<String> kjoenn,
            Optional<FolkeregistrertNavn> visningsnavn
    ) implements Data{
        public record FolkeregistrertNavn(
                String fornavn,
                String mellomnavn,
                String etternavn
        ){}
    }
}