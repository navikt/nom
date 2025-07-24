package no.nav.nom.apps.api.rest.admin;

import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;
import java.util.List;

public record RobotRessursSanityCheckResponse(
    String navident,
    String visningsnavn,
    Sektor sektor,
    LocalDate startdato,
    LocalDate sluttdato,
    List<RobotOrgtilknytningSanityCheckResponse> robotOrgtilknytningSanityCheckResponse
) {

    record RobotOrgtilknytningSanityCheckResponse(
        String orgenhetId,
        String orgenhetNavn,
        LocalDate gyldigFom,
        LocalDate gyldigTom
    ) {}
}
