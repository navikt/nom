package no.nav.nom.apps.testdata.orgtilknytning;

import no.nav.nom.apps.testdata.common.repository.Audit;

import java.time.LocalDateTime;

public record OrgtilknytningEvtsrcMsg(
    long id,
    long testSettId,
    Data data,
    Audit audit
) {
    public record Data(
        Long orgtilknytningStateId,
        long sortIdx,
        LocalDateTime evtsrcTimestamp,
        String objectType,
        String evtsrcKey,
        String evtsrcMessage
    ) {}
}
