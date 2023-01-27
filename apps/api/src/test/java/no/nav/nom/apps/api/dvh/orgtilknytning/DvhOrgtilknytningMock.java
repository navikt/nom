package no.nav.nom.apps.api.dvh.orgtilknytning;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record DvhOrgtilknytningMock(
    @JsonProperty("NAV_ID") String navident,
    @JsonProperty("ORGENHET") String agressoId,
    @JsonProperty("FRA_DATO") LocalDate gyldigFom,
    @JsonProperty("TIL_DATO") LocalDate gyldigTom
) {}
