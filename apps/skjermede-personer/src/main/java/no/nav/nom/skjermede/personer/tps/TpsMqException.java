package no.nav.nom.skjermede.personer.tps;


public class TpsMqException extends RuntimeException {

    public TpsMqException(final String message) {
        super(message);
    }

    public TpsMqException(final String message, Throwable cause) {
        super(message, cause);
    }
}
