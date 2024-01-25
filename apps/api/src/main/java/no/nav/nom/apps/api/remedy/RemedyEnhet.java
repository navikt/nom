package no.nav.nom.apps.api.remedy;

import lombok.Builder;

@Builder(toBuilder = true)
public record RemedyEnhet(
    String remedyEnhetId,
    String agressoId,
    String orgNivaa,
    String nomId
) {
}
