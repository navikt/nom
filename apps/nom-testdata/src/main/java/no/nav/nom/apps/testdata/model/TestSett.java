package no.nav.nom.apps.testdata.model;

public record TestSett(
    long id,
    Data data,
    Audit audit
) {

    public record Data(
        String navn,
        String beskrivelse
    ) {}
}
