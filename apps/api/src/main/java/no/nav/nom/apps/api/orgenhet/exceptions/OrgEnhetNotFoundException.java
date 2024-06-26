package no.nav.nom.apps.api.orgenhet.exceptions;

public class OrgEnhetNotFoundException extends RuntimeException {

    public OrgEnhetNotFoundException(String message) {
        super(message);
    }

    public static OrgEnhetNotFoundException id(String id) {
        return new OrgEnhetNotFoundException(
            String.format("Fant ingen orgenhet med id %s", id)
        );
    }
}
