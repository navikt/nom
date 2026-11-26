package no.nav.nom.apps.testdata.ressurs;

import no.nav.nom.apps.testdata.common.repository.Audit;

import java.time.LocalDateTime;

public record RessursEvtsrcMsg(
    long id,
    long testSettId,
    Data data,
    Audit audit
) {
    public record Data(
        Long ressursStateId,
        long sortIdx,
        LocalDateTime evtsrcTimestamp,
        String objectType,
        String evtsrcKey,
        String evtsrcMessage
    ) {}
}
