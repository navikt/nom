package no.nav.nom.apps.admin.sanitycheck.dto.ressurs;

import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;

public record RessursMedEnhet(
    String navident,
    String ressursFid,
    String visningsnavn,
    LocalDate startdato,
    LocalDate sluttdato,
    Sektor sektor,
    String orgEnhetId,
    String orgEnhetNavn,
    LocalDate gyldigFom,
    LocalDate gyldigTom,
    String uniqueAgressoId
) {
}
