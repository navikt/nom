package no.nav.nom.apps.nom.ressurser.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DvhRessursDTO {

    @JsonProperty("SEKVENSNR")
    private long offset;

    @JsonProperty("NAV_IDENT")
    private String navident;

    @JsonProperty("FNR")
    private String personIdent;

    @JsonProperty("FORNAVN")
    private String fornavn;

    @JsonProperty("ETTERNAVN")
    private String etternavn;

    @JsonProperty("EPOST")
    private String epost;

    @JsonProperty("REMEDY_START_DATO")
    private LocalDate remedyStartDato;

    @JsonProperty("REMEDY_SLUTT_DATO")
    private LocalDate remedySluttDato;

    @JsonProperty("REMEDY_PERMISJON")
    private String permittert;

    @JsonProperty("AGRESSO_SLUTT_DATO")
    private LocalDate agressoSluttDato;

    @JsonProperty("ANSATT_TYPE")
    private String ansattType;

    @JsonProperty("ANSATT_RESSURS_KODE")
    private String ressursTypeKode;
}