package no.nav.nom.apps.api.orgenhetleder.api.app.exception;

public class OrgEnhetLederNotFoundException extends RuntimeException {
    public OrgEnhetLederNotFoundException(String message) {
        super(message);
    }

    public static OrgEnhetLederNotFoundException orgEnhetId(String orgEnhetId) {
        return new OrgEnhetLederNotFoundException(String.format("Kunne ikke finne en OrgEnhetLederState med id %s", orgEnhetId));
    }
}
