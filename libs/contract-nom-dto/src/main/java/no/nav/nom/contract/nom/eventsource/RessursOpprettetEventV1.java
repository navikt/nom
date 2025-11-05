package no.nav.nom.contract.nom.eventsource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;
import java.util.List;

public class RessursOpprettetEventV1 extends RessursEvent {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RessursOpprettetEventV1(
        @JsonProperty("fid") String fid,
        @JsonProperty("data") RessursOpprettetEventData data,
        @JsonProperty("metadata") Metadata metadata,
        @JsonProperty("additionalData") RessursOpprettetAdditionalData additionalData
    ) {
        super(fid, data, metadata, additionalData);
    }

    @JsonIgnore
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
        String primaryTelefonnummer,
        List<String> fysiskeBrukersteder,
        String primaryFysiskBrukersted,
        IdentType identType,
        Tilknytningtype tilknytningtype
    ) implements Data {}

    @Builder(toBuilder = true)
    public record RessursOpprettetAdditionalData(
        String navident,
        String kjoenn,
        FolkeregistrertNavn folkeregistrertNavn,
        LocalDate foedselsdato,
        LocalDate startdato,
        String orgEnhet,
        AnsettelsesType ansettelsesType
    ) implements Data {
        public record FolkeregistrertNavn(
            String fornavn,
            String mellomnavn,
            String etternavn
        ) {}
    }
}