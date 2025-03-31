package no.nav.nom.apps.api.orgtilknytning.api.app;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MasseFlyttingResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<FeiletFlytting> feiledeFlyttinger,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int antallFlyttet
) {
    public record FeiletFlytting(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgtilknytningFid,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String feilmelding
    ) {
    }
}
