package no.nav.nom.apps.api.clients.koststed;

import java.time.LocalDate;

public record KostnadsstedResponseDTO(
    String kostnadsstedId,
    String orgenhetId,
    LocalDate gyldigFom,
    LocalDate gyldigTom
) {
}
