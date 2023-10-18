package no.nav.nom.apps.api.person.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record Person(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String personident,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String navident,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) FolkeregisterPerson folkeregisterPerson
) { }


