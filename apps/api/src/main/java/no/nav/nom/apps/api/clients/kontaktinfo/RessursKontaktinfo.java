package no.nav.nom.apps.api.clients.kontaktinfo;

import lombok.Builder;

import java.util.List;

@Builder
public record RessursKontaktinfo(
    String ressursNavident,
    List<Kontaktinfo> kontaktinfo
) {
}
