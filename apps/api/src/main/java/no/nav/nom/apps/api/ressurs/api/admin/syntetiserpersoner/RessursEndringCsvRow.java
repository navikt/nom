package no.nav.nom.apps.api.ressurs.api.admin.syntetiserpersoner;

public record RessursEndringCsvRow(
    String fid,
    String fornavn,
    String etternavn,
    String personident
) {
    public boolean hasPersonident() {
        return personident != null && !personident.isBlank();
    }

    public boolean hasNavn() {
        return (fornavn != null && !fornavn.isBlank()) || (etternavn != null && !etternavn.isBlank());
    }
}
