package no.nav.nom.apps.api.rest.navident;

import lombok.Data;

@Data
public class NavidentRequest {

    private String personident;
    private String fornavn;
    private String etternavn;
    private String forbokstav;
    private String sektor;
}
