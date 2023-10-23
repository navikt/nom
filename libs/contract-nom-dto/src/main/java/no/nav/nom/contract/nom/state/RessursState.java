package no.nav.nom.contract.nom.state;

import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.util.Optional;

public record RessursState(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String personident,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Sektor sektor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Visningsnavn visningsnavn,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<LocalDate> sluttdato,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<LocalDate> startdato
) {}
