package no.nav.nom.apps.api.ressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdatePrimarytelefonnummer(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nummer
) {
}
