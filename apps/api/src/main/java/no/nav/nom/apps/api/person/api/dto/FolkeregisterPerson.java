package no.nav.nom.apps.api.person.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record FolkeregisterPerson(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RessursNavn navn
) {
    public record RessursNavn(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fornavn,
            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String mellomnavn,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String etternavn
    ) {}
}

