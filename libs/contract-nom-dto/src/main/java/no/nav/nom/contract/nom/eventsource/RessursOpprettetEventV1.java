package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;
import java.util.Optional;

public class RessursOpprettetEventV1 extends RessursEvent {

    @JsonCreator
    public RessursOpprettetEventV1(String fid, RessursOpprettetEventData data, Metadata metadata, RessursOpprettetAdditionalData additionalData){
        super(fid, data, metadata, additionalData);
    }

    public RessursOpprettetEventV1(String fid, RessursOpprettetEventData data, InputMetadata inputMetadata, RessursOpprettetAdditionalData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    public record RessursOpprettetEventData(
            String personident,
            Sektor sektor,
            Visningsnavn visningsnavn
    ) implements Data{ }

    public record RessursOpprettetAdditionalData(
            Optional<String> navident,
            Optional<String> kjoenn,
            Optional<FolkeregistrertNavn> folkeregistrertNavn,
            Optional<LocalDate> foedselsdato,
            Optional<LocalDate> startdato
    ) implements Data {
        public record FolkeregistrertNavn(
                String fornavn,
                String mellomnavn,
                String etternavn
        ){}
    }
}