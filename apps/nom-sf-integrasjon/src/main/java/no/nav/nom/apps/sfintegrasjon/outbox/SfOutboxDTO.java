package no.nav.nom.apps.sfintegrasjon.outbox;

import lombok.Builder;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.SfOrgEnhet;

import java.util.Set;

@Builder(toBuilder = true)
public record SfOutboxDTO(
    String sporingsId,
    Set<String> endretAv,
    Set<String> endringshendelser,
    SfOrgEnhet sfOrgEnhet
) {
}
