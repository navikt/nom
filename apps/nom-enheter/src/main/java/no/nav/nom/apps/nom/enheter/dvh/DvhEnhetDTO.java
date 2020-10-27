package no.nav.nom.apps.nom.enheter.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DvhEnhetDTO {

    @JsonProperty("SEKVENSNR")
    private long offset;

    @JsonProperty("ORGNODE_KODE")
    private String enhetKode;

    @JsonProperty("ORGNODE_NIVA")
    private String nodeNivaa;

    @JsonProperty("ORGNODE_TYPE")
    private String enhetType;

    @JsonProperty("ORGNODE_NAVN")
    private String enhetNavn;

    @JsonProperty("AGRESSONR")
    private String agressoNr;

    @JsonProperty("REMEDYNR")
    private String remedyNr;

    @JsonProperty("GYLDIG_FRA")
    private LocalDate enhetGyldigFra;

    @JsonProperty("GYLDIG_TIL")
    private LocalDate enhetGyldigTil;

    @JsonProperty("ORGNODE_KODE_OVER")
    private String overEnhetKode;

    @JsonProperty("ORGNODE_NAVN_OVER")
    private String overEnhetNavn;

    @JsonProperty("ORGNODE_NIVA_OVER")
    private String nodeNivaaOver;

    @JsonProperty("GYLDIG_FRA_ORG")
    private LocalDate orgGyldigFra;

    @JsonProperty("GYLDIG_TIL_ORG")
    private LocalDate orgGyldigTil;

    @JsonProperty("ORGANISERING_TYPE")
    private String organiseringsType;

    @JsonProperty(value = "ORGNODE_STATUS")
    private String orgNodeStatus;

    @JsonProperty(value = "LEDER_RESOURCE_ID")
    private String lederRessursId;

    @JsonProperty(value = "LEDER_USER_ID")
    private String lederBrukerId;
}