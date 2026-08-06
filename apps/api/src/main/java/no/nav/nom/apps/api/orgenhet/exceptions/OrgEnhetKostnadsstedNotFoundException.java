package no.nav.nom.apps.api.orgenhet.exceptions;

public class OrgEnhetKostnadsstedNotFoundException extends RuntimeException {
    public OrgEnhetKostnadsstedNotFoundException(String message) {
        super(message);
    }

    public static OrgEnhetKostnadsstedNotFoundException notFound(String orgEnhetId, String orgEnhetNavn) {
        return new OrgEnhetKostnadsstedNotFoundException(String.format("Fant ikke koststedId for %s (%s), mangler muligens mapping", orgEnhetNavn, orgEnhetId));
    }
}
