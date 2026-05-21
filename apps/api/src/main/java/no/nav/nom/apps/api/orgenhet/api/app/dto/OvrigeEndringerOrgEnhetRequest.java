package no.nav.nom.apps.api.orgenhet.api.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record OvrigeEndringerOrgEnhetRequest(
    @Schema(example = "ab123c", requiredMode = Schema.RequiredMode.REQUIRED) String id,
    @Schema(example = "2025-01-01", requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate gyldigFom,
    @Schema(example = "DIREKTORAT", requiredMode = Schema.RequiredMode.NOT_REQUIRED) OrgEnhetsType orgEnhetsType,
    @Schema(example = "1234", requiredMode = Schema.RequiredMode.NOT_REQUIRED) String tilgangsenhetId,
    @Schema(example = "ARBEIDSOMRAADE", requiredMode = Schema.RequiredMode.NOT_REQUIRED) NomNivaa nomNivaa
) {
}
