package no.nav.nom.apps.api.clients.kontaktinfo;

import lombok.Builder;

@Builder
public record Kontaktinfo(
    String kontaktId,
    String fornavn,
    String etternavn,
    String relasjon,
    String telefon,
    Integer prioritet,
    String gateadresse,
    String postnr,
    String poststed,
    String land
) {}
