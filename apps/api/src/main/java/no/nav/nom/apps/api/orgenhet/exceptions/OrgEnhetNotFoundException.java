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

    public static OrgEnhetNotFoundException fid(String fid) {
        return new OrgEnhetNotFoundException(
            String.format("Fant ingen orgenhet med fid %s", fid)
        );
    }

    public static OrgEnhetNotFoundException agressoIdNivaaNavn(String agressoId, String nivaa, String navn) {
        return new OrgEnhetNotFoundException(
            String.format("Fant ikke orgenhet med agressoId: %s, nivå: %s og navn: %s kombinasjon", agressoId, nivaa, navn)
        );
    }
}
