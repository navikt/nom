package no.nav.nom.apps.api.orgenhet.api.app.dto;

import lombok.Builder;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record OpprettOrgEnhetRequest(
    String overordnetOrgEnhetId,
    String orgEnhetNavn,
    LocalDate gyldigFom,
    String lederRessursNavident,
    String firesiffer,
    OrgEnhetsType orgEnhetsType
) {}
