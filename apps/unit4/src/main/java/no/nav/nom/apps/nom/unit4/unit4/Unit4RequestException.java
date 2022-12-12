package no.nav.nom.apps.nom.unit4.unit4;

import no.nav.nom.apps.nom.unit4.unit4.Unit4Exception;

public class Unit4RequestException extends Unit4Exception {
    public Unit4RequestException(String message) {
        super(message);
    }

    public Unit4RequestException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
