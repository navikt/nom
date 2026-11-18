package no.nav.nom.apps.testdata.model;

import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrgtilknytningState(
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
        Sektor sektor,
        boolean erDagligOppfolging,
        LocalDate gyldigFom,
        LocalDate gyldigTom,
        Tilknytningtype orgtilknytningType,
        String primaryFysiskBrukerstedNummer,
        List<String> additionalFysiskeBrukerstederNummer,
        String masseflyttetFraFid,
        String stateSistEndretAv
    ) {}
}
