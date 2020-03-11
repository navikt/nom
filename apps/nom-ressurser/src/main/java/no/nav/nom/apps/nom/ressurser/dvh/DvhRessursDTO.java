package no.nav.nom.apps.nom.ressurser.dvh;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DvhRessursDTO {

    private long sekvensnr;

    private String nav_ident; // Nødvendig

    private String fnr; // Nødvendig

    private String fornavn; // ikke nødvendig?

    private String etternavn; // ikke nødvendig?

    private String epost; // nødvendig

    private LocalDate remedy_start_dato; // nødvendig

    private LocalDate remedy_slutt_dato; // nødvendig

    private LocalDate agresso_slutt_dato; // Ikke nødvendig?

    private boolean ansatt_per_idag; // ikke nødvendig

    private String ansettelsestype_besk; // ikke nødvendig?

    private String ansatt_type; // nødvendig
}