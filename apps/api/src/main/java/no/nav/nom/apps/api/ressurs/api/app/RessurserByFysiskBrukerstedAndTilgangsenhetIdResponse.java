package no.nav.nom.apps.api.ressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

public record RessurserByFysiskBrukerstedAndTilgangsenhetIdResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String navident,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    Visningsnavn visningsnavn,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String orgEnhetId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String orgEnhetNavn,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String tilgangsenhetId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String fysiskBrukerstedNummer,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String fysiskBrukerstedNavn
) {
}
