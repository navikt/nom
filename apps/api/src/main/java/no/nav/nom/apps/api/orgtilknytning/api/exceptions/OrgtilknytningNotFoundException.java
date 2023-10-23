package no.nav.nom.apps.api.orgtilknytning.api.exceptions;

public class OrgtilknytningNotFoundException extends RuntimeException {

    public OrgtilknytningNotFoundException(String message) {
        super(message);
    }

    public static OrgtilknytningNotFoundException fid(String fid) {
        return new OrgtilknytningNotFoundException(
            String.format("Fant ingen orgtilknytning med fid %s", fid)
        );
    }
}
