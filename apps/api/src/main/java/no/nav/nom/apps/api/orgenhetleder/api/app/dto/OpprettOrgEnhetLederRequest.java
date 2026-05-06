package no.nav.nom.apps.api.orgenhetleder.api.app.dto;

import java.time.LocalDate;

public record OpprettOrgEnhetLederRequest(
    String orgEnhetId,
    String navident,
    LocalDate gyldigFom,
    LocalDate gyldigTom
) {
}
