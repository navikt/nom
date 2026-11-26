package no.nav.nom.apps.testdata.orgenhetleder;

import no.nav.nom.apps.testdata.common.repository.Audit;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrgEnhetLederState(
    long id,
    long testSettId,
    Data data,
    Audit audit
) {
    public record Data(
        long sortIdx,
        LocalDateTime stateTimestamp,
        String fid,
        String navident,
        String orgEnhetId,
        LocalDate startdato,
        LocalDate sluttdato,
        String stateSistEndretAv
    ) {}
}
