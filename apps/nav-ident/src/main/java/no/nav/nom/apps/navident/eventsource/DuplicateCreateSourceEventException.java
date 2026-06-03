package no.nav.nom.apps.navident.eventsource;

public class DuplicateCreateSourceEventException extends RuntimeException {

    public DuplicateCreateSourceEventException(String format, Object... args) {
        super(String.format(format, args));
    }
}
