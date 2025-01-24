package no.nav.nom.apps.api.clients.msgraph;

public class EmailSendingUnforeseenException extends RuntimeException {

    public EmailSendingUnforeseenException(String message) {
        super(message);
    }
}
