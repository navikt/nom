package no.nav.nom.apps.api.organisering.exceptions;

public class OrganiseringNotFoundException extends RuntimeException {

    public OrganiseringNotFoundException(String message) {
        super(message);
    }

    public static OrganiseringNotFoundException id(String id) {
        return new OrganiseringNotFoundException(
            String.format("Organisering id %s ble ikke funnet", id)
        );
    }
}
