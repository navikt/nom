package no.nav.nom.apps.testdata.organisering;

import no.nav.nom.apps.testdata.common.repository.Audit;

import java.time.LocalDateTime;

public record OrganiseringEvtsrcMsg(
    long id,
    long testSettId,
    Data data,
    Audit audit
) {
    public record Data(
        Long organiseringStateId,
        long sortIdx,
        LocalDateTime evtsrcTimestamp,
        String objectType,
        String evtsrcKey,
        String evtsrcMessage
    ) {}
}
