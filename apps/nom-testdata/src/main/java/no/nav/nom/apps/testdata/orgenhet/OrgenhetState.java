package no.nav.nom.apps.testdata.orgenhet;

import no.nav.nom.apps.testdata.common.repository.Audit;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrgenhetState(
    long id,
    long testSettId,
    Data data,
    Audit audit
) {

    public record Data(
        long sortIdx,
        LocalDateTime stateTimestamp,
        String fid,
        String orgenhetId,
        String navn,
        LocalDate gyldigFom,
        LocalDate gyldigTom,
        OrgEnhetsType orgenhetsType,
        NomNivaa nomNivaa,
        String uniqueAgressoId,
        String agressoId,
        String agressoOrgNivaa,
        String agressoOrgenhetType,
        String tilgangsenhetId,
        String kostnadsstedId
    ) {}
}
