package no.nav.nom.apps.nom.ressurser.ressurserapi;

import lombok.Getter;
import no.nav.nom.contract.nom.dto.Ressurstype;

import java.time.LocalDate;

@Getter
public class RessursRequestDTO {
    private String personident;
    private String navident;
    private String fornavn;
    private String etternavn;
    private String epost;
    private Ressurstype ressurstype;
    private String startdato;
    private String sluttdato;
}
