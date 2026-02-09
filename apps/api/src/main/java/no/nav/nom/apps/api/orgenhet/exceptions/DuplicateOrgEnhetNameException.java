package no.nav.nom.apps.api.orgenhet.exceptions;

public class DuplicateOrgEnhetNameException extends RuntimeException {
    public DuplicateOrgEnhetNameException(String message) {
        super(message);
    }

    public static DuplicateOrgEnhetNameException name(String name) {
        return new DuplicateOrgEnhetNameException(String.format("Navnet [%s] finnes allerede som aktiv enhet i NOM", name));
    }
}
