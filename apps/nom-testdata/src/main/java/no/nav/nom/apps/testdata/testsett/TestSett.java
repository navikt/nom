package no.nav.nom.apps.testdata.testsett;

import no.nav.nom.apps.testdata.common.repository.Audit;

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
