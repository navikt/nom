package no.nav.nom.apps.api.authorize.redigereressurs;

public class NotAuthorizedToDelegateLeder extends RuntimeException {
    public NotAuthorizedToDelegateLeder(String message) {
        super(message);
    }
}
