package no.nav.nom.apps.admin.sanitycheck.dto.ressurs;

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

    public record RobotOrgtilknytningSanityCheckResponse(
            String orgenhetId,
            String orgenhetNavn,
            LocalDate gyldigFom,
            Optional<LocalDate> gyldigTom
    ) {}
}
