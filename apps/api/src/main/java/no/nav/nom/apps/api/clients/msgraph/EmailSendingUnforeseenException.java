package no.nav.nom.apps.api.clients.msgraph;

public class EmailSendingUnforeseenException extends RuntimeException {

    public EmailSendingUnforeseenException(String message) {
        super(message);
    }

    public static void emailNotFound(String navident) {
        throw new EmailSendingUnforeseenException(String.format("Could not find email address for navident '%s'.", navident));
    }
}
