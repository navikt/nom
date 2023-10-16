package no.nav.nom.apps.api.person.api.dto;

public record FolkeregisterPerson(
    RessursNavn navn
) {
    public record RessursNavn(
        String fornavn,
        String mellomnavn,
        String etternavn
    ) {}
}

