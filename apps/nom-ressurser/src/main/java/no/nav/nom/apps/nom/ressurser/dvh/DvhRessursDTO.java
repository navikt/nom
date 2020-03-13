package no.nav.nom.apps.nom.ressurser.dvh;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DvhRessursDTO {

    public long sekvensnr;

    public String nav_ident; // Nødvendig

    public String fnr; // Nødvendig

    public String fornavn; // ikke nødvendig?

    public String etternavn; // ikke nødvendig?

    public String epost; // nødvendig

    public LocalDate remedy_start_dato; // nødvendig

    public LocalDate remedy_slutt_dato; // nødvendig

    public LocalDate agresso_slutt_dato; // Ikke nødvendig?

    public boolean ansatt_per_idag; // ikke nødvendig

    public String ansettelsestype_besk; // ikke nødvendig?

    public String ansatt_type; // nødvendig
}