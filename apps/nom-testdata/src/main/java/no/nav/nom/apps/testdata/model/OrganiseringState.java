package no.nav.nom.apps.testdata.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrganiseringState(
    long id,
    long testSettId,
    Data data,
    Audit audit
) {
    public record Data(
        long sortIdx,
        LocalDateTime stateTimestamp,
        String fid,
        String organiseringId,
        String navn,
        String idOver,
        String navnOver,
        LocalDate gyldigFom,
        LocalDate gyldigTom
    ) {}
}
