package no.nav.nom.apps.api.orgenhet.api.app.dto;

import lombok.Builder;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record OpprettOrgEnhetResponse(
    String orgEnhetFid,
    String orgEnhetId,
    String orgEnhetNavn,
    String overordnetOrgEnhetId,
    String overordnetOrgEnhetNavn,
    LocalDate gyldigFom,
    LocalDate gyldigTom,
    String lederRessursNavident,
    String lederRessursNavn,
    String firesiffer,
    OrgEnhetsType orgEnhetsType
) {
}
