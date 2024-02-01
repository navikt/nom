package no.nav.nom.apps.api.orgtilknytning.api.exceptions;

import java.time.LocalDate;
import java.util.Optional;

public class OrgtilknytningPeriodeOverlapException extends RuntimeException {

    public OrgtilknytningPeriodeOverlapException(String message) {
        super(message);
    }

    public static OrgtilknytningPeriodeOverlapException forDagligOppfolgingInPeriod(String dagligOppfolgingOrgenhetNavn, LocalDate gyldigFom, Optional<LocalDate> gyldigTom) {
        var message = "Det finnes allerede en daglig oppfølgingsenhet, '%s', som gjelder i perioden %s - %s. Du må sette gyldig t.o.m på den eksisterende orgtilknytningen slik at den ikke overlapper med den nye orgtilknytningen."
            .formatted(
                dagligOppfolgingOrgenhetNavn,
                gyldigFom,
                gyldigTom.map(LocalDate::toString).orElse("Løpende")
            );

        return new OrgtilknytningPeriodeOverlapException(message);
    }

}
