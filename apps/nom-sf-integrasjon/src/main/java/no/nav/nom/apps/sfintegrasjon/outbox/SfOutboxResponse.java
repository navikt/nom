package no.nav.nom.apps.sfintegrasjon.outbox;

import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.SfOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfStatus;
import tools.jackson.databind.JsonNode;

import java.util.Set;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record SfOutboxResponse(
    @Schema(requiredMode = REQUIRED)
    Long sporingsId,
    @Schema(requiredMode = REQUIRED)
    SfStatus sfStatus,
    String feilmelding,
    @Schema(requiredMode = REQUIRED)
    String orgEnhetId,
    Set<String> endretAv,
    Set<String> endringshendelser,
    @Schema(
        requiredMode = REQUIRED,
        implementation = SfOrgEnhet.class,
        description = "Orgenheten slik den ligger i outboxen. Avviste rader kan ha JSON som ikke følger SfOrgEnhet."
    )
    JsonNode orgenhet
) {
}
