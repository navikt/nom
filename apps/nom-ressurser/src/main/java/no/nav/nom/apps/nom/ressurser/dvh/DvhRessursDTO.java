package no.nav.nom.apps.nom.ressurser.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;


import java.time.LocalDate;


public interface DvhRessursDTO {
    long getOffset();
    String getNavident();
    String getPersonIdent();
    String getFornavn();
    String getEtternavn();
    String getEpost();
    LocalDate getRemedyStartDato();
    LocalDate getRemedySluttDato();
    String getPermisjon();
    LocalDate getAgressoSluttDato();
    String getAnsattType();
    String getRessursTypeKode();
}
