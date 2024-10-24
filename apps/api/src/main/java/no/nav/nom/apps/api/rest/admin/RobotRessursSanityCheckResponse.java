package no.nav.nom.apps.api.rest.admin;

import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record RobotRessursSanityCheckResponse(
    String navident,
    String visningsnavn,
    Sektor sektor,
    Optional<LocalDate> startdato,
    Optional<LocalDate> sluttdato,
    List<RobotOrgtilknytningSanityCheckResponse> robotOrgtilknytningSanityCheckResponse
) {

    record RobotOrgtilknytningSanityCheckResponse(
        String orgenhetId,
        String orgenhetNavn,
        LocalDate gyldigFom,
        Optional<LocalDate> gyldigTom
    ) {}
}
