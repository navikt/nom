package no.nav.nom.apps.admin.dto.orgenhet;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Builder(toBuilder = true)
public record OrgEnhetStateDTO(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String key,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime timestamp,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navn,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<LocalDate> gyldigTom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<NomNivaa> nomNivaa,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<OrgEnhetsType> orgEnhetsType
) {}
