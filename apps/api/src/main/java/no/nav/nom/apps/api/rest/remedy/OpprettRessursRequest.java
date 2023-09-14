package no.nav.nom.apps.api.rest.remedy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OpprettRessursRequest {
    private String personident;
    private String fornavn;
    private String mellomnavn;
    private String etternavn;
    private String forbokstav;
    private String identType;
    private String sektor;
    private String submitter;
}
