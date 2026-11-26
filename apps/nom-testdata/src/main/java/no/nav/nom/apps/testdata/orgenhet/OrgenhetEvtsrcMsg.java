package no.nav.nom.apps.testdata.orgenhet;

import no.nav.nom.apps.testdata.common.repository.Audit;

import java.time.LocalDateTime;

public record OrgenhetEvtsrcMsg(
    long id,
    long testSettId,
    Data data,
    Audit audit
) {

    public record Data(
        Long orgenhetStateId,
        long sortIdx,
        LocalDateTime evtsrcTimestamp,
        String objectType,
        String evtsrcKey,
        String evtsrcMessage
    ) {}
}
