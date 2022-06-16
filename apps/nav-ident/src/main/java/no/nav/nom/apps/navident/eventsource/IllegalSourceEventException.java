package no.nav.nom.apps.navident.eventsource;

public class IllegalSourceEventException extends RuntimeException {

    public IllegalSourceEventException(String format, Object... args) {
        super(String.format(format, args));
    }
}
