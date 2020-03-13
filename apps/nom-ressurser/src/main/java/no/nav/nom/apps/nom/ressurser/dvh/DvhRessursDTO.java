package no.nav.nom.apps.nom.ressurser.dvh;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DvhRessursDTO {

    public long SEKVENSNR;

    public String NAV_IDENT; // Nødvendig

    public String FNR; // Nødvendig

    public String FORNAVN; // ikke nødvendig?

    public String ETTERNAVN; // ikke nødvendig?

    public String EPOST; // nødvendig

    public LocalDate REMEDY_START_DATO; // nødvendig

    public LocalDate REMEDY_SLUTT_DATO; // nødvendig

    public LocalDate AGRESSO_SLUTT_DATO; // Ikke nødvendig?

    public boolean ANSATT_PER_IDAG; // ikke nødvendig

    public String ANSETTELSESTYPE_BESK; // ikke nødvendig?

    public String ANSATT_TYPE; // nødvendig
}