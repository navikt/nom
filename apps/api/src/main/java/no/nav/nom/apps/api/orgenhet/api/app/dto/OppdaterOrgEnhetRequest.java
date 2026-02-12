package no.nav.nom.apps.api.orgenhet.api.app.dto;

import lombok.Builder;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record OppdaterOrgEnhetRequest(
    String id,
    String navn,
    LocalDate gyldigFom,
    LocalDate gyldigTom,
    String lederRessursNavident,
    String firesiffer,
    OrgEnhetsType orgEnhetsType
) {
}
