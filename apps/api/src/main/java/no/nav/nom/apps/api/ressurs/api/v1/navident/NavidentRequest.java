package no.nav.nom.apps.api.ressurs.api.v1.navident;

import lombok.Data;

@Data
public class NavidentRequest {

    private String personident;
    private String fornavn;
    private String mellomnavn;
    private String etternavn;
    private String forbokstav;
    private String identType;
    private String sektor;
    private String submitter;
}
