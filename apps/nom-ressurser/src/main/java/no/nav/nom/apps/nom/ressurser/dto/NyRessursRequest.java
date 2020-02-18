package no.nav.nom.apps.nom.ressurser.dto;

import lombok.Getter;
import no.nav.nom.contract.nom.dto.Ressurstype;

import java.time.LocalDate;

@Getter
public class NyRessursRequest {

    private String personident;

    private Ressurstype ressurstype;

    private LocalDate startdato;

    private LocalDate sluttdato;

}
