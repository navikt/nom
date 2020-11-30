package no.nav.nom.contract.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class Enhet {
    public static final String TYPE = "no.nav.nom.dvh:Enhet";

    @JsonProperty("SEKVENSNR")
    long offset;

    @JsonProperty("ORGNODE_KODE")
    String enhetKode;

    @JsonProperty("ORGNODE_NIVA")
    String nodeNivaa;

    @JsonProperty("ORGNODE_TYPE")
    String enhetType;

    @JsonProperty("ORGNODE_NAVN")
    String enhetNavn;

    @JsonProperty("AGRESSONR")
    String agressoNr;

    @JsonProperty("REMEDYNR")
    String remedyNr;

    @JsonProperty("GYLDIG_FRA")
    LocalDate enhetGyldigFra;

    @JsonProperty("GYLDIG_TIL")
    LocalDate enhetGyldigTil;

    @JsonProperty("ORGNODE_KODE_OVER")
    String overEnhetKode;

    @JsonProperty("ORGNODE_NAVN_OVER")
    String overEnhetNavn;

    @JsonProperty("ORGNODE_NIVA_OVER")
    String nodeNivaaOver;

    @JsonProperty("GYLDIG_FRA_ORG")
    LocalDate orgGyldigFra;

    @JsonProperty("GYLDIG_TIL_ORG")
    LocalDate orgGyldigTil;

    @JsonProperty("ORGANISERING_TYPE")
    String organiseringsType;

    @JsonProperty(value = "ORGNODE_STATUS")
    String orgNodeStatus;

    @JsonProperty(value = "LEDER_RESOURCE_ID")
    String lederRessursId;

    @JsonProperty(value = "LEDER_USER_ID")
    String lederBrukerId;
}
