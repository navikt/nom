package no.nav.nom.contract.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class Ressurs {

    public static final String TYPE = "no.nav.nom.dvh:Ressurs";

    @JsonProperty("SEKVENSNR")
    long offset;

    @JsonProperty("NAV_IDENT")
    String navident;

    @JsonProperty("FNR")
    String personIdent;

    @JsonProperty("FORNAVN")
    String fornavn;

    @JsonProperty("ETTERNAVN")
    String etternavn;

    @JsonProperty("EPOST")
    String epost;

    @JsonProperty("REMEDY_START_DATO")
    LocalDate remedyStartDato;

    @JsonProperty("REMEDY_SLUTT_DATO")
    LocalDate remedySluttDato;

    @JsonProperty("REMEDY_PERMISJON")
    String permisjon;

    @JsonProperty("AGRESSO_SLUTT_DATO")
    LocalDate agressoSluttDato;

    @JsonProperty("ANSATT_TYPE")
    String ansattType;

    @JsonProperty("ANSATT_RESSURS_KODE")
    String ressursTypeKode;
}
