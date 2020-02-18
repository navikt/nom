package no.nav.nom.apps.nom.ressurser.provider.rs;

import lombok.Getter;
import no.nav.nom.contract.nom.dto.Ressurstype;

import java.time.LocalDate;

@Getter
public class NyRessursRequest {

    private String personident;

    private Ressurstype ressurstype;

    private String startdato;

    private String sluttdato;

}
