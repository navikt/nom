package no.nav.nom.apps.api.clients.koststed;

import java.time.LocalDate;

public record CreateOrgenhetKostnadsstedMappingExternalRequest(
    String kostnadsstedId,
    String orgenhetId,
    LocalDate gyldigFom,
    LocalDate gyldigTom
) {
}
