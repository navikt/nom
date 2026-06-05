package no.nav.nom.apps.api.ressurs.api.v1.navident;

import lombok.Data;
import no.nav.nom.contract.nom.IdentType;

@Data
public class NavidentRequest {

    private String personident;
    private IdentType identType;
}