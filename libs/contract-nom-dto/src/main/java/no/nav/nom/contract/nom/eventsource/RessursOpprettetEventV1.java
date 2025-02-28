package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class RessursOpprettetEventV1 extends RessursEvent {

    @JsonCreator
    public RessursOpprettetEventV1(String fid, RessursOpprettetEventData data, Metadata metadata, RessursOpprettetAdditionalData additionalData) {
        super(fid, data, metadata, additionalData);
    }

    public RessursOpprettetEventV1(String fid, RessursOpprettetEventData data, InputMetadata inputMetadata, RessursOpprettetAdditionalData additionalData) {
        this(fid, data, Metadata.from(inputMetadata), additionalData);
    }

    @JsonIgnore
    public RessursOpprettetEventData getRessursOpprettetEventData() {
        return (RessursOpprettetEventData) getData();
    }

    @JsonIgnore
    public RessursOpprettetAdditionalData getRessursOpprettetAdditionalData() {
        return (RessursOpprettetAdditionalData) getAdditionalData();
    }

    @Builder(toBuilder = true)
    public record RessursOpprettetEventData(
        String personident,
        Sektor sektor,
        Visningsnavn visningsnavn,
        Optional<String> primaryTelefonnummer,
        List<String> fysiskeBrukersteder,
        String primaryFysiskBrukersted,
        IdentType identType
    ) implements Data {}

    @Builder(toBuilder = true)
    public record RessursOpprettetAdditionalData(
        Optional<String> navident,
        Optional<String> kjoenn,
        Optional<FolkeregistrertNavn> folkeregistrertNavn,
        Optional<LocalDate> foedselsdato,
        Optional<LocalDate> startdato,
        Optional<String> orgEnhet,
        Optional<AnsettelsesType> ansettelsesType
    ) implements Data {
        public record FolkeregistrertNavn(
            String fornavn,
            String mellomnavn,
            String etternavn
        ) {}
    }
}