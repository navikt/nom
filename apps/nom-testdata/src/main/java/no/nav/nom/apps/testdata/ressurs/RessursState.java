package no.nav.nom.apps.testdata.ressurs;

import no.nav.nom.apps.testdata.common.repository.Audit;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RessursState(
    long id,
    long testSettId,
    Data data,
    Audit audit
) {
    public record Data(
        long sortIdx,
        LocalDateTime stateTimestamp,
        String fid,
        String personident,
        String navident,
        Sektor sektor,
        String fornavn,
        String mellomnavn,
        String etternavn,
        String jobbtelefonNummer,
        TelefonType jobbtelefonType,
        String primaryTelefonnummer,
        LocalDate sluttdato,
        LocalDate startdato,
        String primaryFysiskBrukersted,
        List<String> fysiskeBrukersteder,
        IdentType identType,
        String stateSistEndretAv,
        Tilknytningtype tilknytningtype
    ) {}
}
