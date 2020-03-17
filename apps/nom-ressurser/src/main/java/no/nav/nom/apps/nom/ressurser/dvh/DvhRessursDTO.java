package no.nav.nom.apps.nom.ressurser.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DvhRessursDTO {

    @JsonProperty("SEKVENSNR")
    private long offset;

    @JsonProperty("NAV_IDENT")
    private String navIdent;

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

    @JsonProperty("AGRESSO_SLUTT_DATO")
    private LocalDate agressoSluttDato;
}