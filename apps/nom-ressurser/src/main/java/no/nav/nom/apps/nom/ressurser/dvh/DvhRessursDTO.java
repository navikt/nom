package no.nav.nom.apps.nom.ressurser.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DvhRessursDTO {

    @JsonProperty("SEKVENSNR")
    private long offset;

    @JsonProperty("NAV_IDENT")
    private String navIdent; // Nødvendig

    @JsonProperty("FNR")
    private String personIdent; // Nødvendig

    @JsonProperty("FORNAVN")
    private String fornavn; // ikke nødvendig?

    @JsonProperty("ETTERNAVN")
    private String etternavn; // ikke nødvendig?

    @JsonProperty("EPOST")
    private String epost; // nødvendig

    @JsonProperty("REMEDY_START_DATO")
    private LocalDate remedyStartDato; // nødvendig

    @JsonProperty("REMEDY_SLUTT_DATO")
    private LocalDate remedySluttDato; // nødvendig

    @JsonProperty("AGRESSO_SLUTT_DATO")
    private LocalDate agressoSluttDato; // Ikke nødvendig?

    @JsonProperty("ANSATT_PER_IDAG")
    private boolean isAnsatt; // ikke nødvendig

    @JsonProperty("ANSETTELSESTYPE_BESK")
    private String ansettelsesTypeBeskrivelse; // ikke nødvendig?

    @JsonProperty("ANSATT_TYPE")
    private String ansattType; // nødvendig
}