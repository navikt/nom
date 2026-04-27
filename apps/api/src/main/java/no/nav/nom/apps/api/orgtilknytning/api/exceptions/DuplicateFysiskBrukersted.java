package no.nav.nom.apps.api.orgtilknytning.api.exceptions;

public class DuplicateFysiskBrukersted extends RuntimeException {
    public DuplicateFysiskBrukersted(String message) {
        super(message);
    }

    public static DuplicateFysiskBrukersted duplicatePrimaryFysiskBrukersted(String fysiskBrukerstedNummer, String fysiskBrukerstedNavn) {
        return new DuplicateFysiskBrukersted(
            "Fysisk brukersted %s (%s) er allerede satt som primær fysisk brukersted".formatted(fysiskBrukerstedNavn, fysiskBrukerstedNummer)
        );
    }
}
