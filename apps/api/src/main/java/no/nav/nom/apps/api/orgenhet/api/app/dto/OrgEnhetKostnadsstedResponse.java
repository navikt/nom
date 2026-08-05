package no.nav.nom.apps.api.orgenhet.api.app.dto;

import java.time.LocalDate;

public record OrgEnhetKostnadsstedResponse(
    String kostnadsstedId,
    String orgenhetId,
    LocalDate gyldigFom,
    LocalDate gyldigTom
) {
}
