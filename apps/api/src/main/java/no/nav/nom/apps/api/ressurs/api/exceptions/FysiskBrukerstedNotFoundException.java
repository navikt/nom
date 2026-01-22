package no.nav.nom.apps.api.ressurs.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
public class FysiskBrukerstedNotFoundException extends RuntimeException {
    public FysiskBrukerstedNotFoundException(String message) {
        super(message);
    }

    public static void fysiskBrukerstedIsNull(String visningnavn, String navident) {
        throw new FysiskBrukerstedNotFoundException(String.format("Ansatt %s (%s) mangler fysisk brukersted, det må rettes opp før det kan gjøres andre endringer. Trykk 'Endre personopplysninger' og velg riktig fysisk brukersted.", visningnavn, navident));
    }
}
