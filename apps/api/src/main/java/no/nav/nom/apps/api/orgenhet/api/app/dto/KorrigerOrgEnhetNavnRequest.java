package no.nav.nom.apps.api.orgenhet.api.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder(toBuilder = true)
public record KorrigerOrgEnhetNavnRequest(
    @Schema(example = "ab123c", requiredMode = Schema.RequiredMode.REQUIRED) String id,
    @Schema(example = "Nav Lokal Kontor", requiredMode = Schema.RequiredMode.REQUIRED) String navn
) {
}
