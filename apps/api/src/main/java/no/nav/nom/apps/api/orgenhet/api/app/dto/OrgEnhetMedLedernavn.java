package no.nav.nom.apps.api.orgenhet.api.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrgEnhetMedLedernavn(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navn,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String remedyEnhetId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> ledere,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom
) { }
