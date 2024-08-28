package no.nav.nom.apps.admin.dto.orgtilknytning;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Builder(toBuilder = true)
public record OrgtilknytningStateDTO(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String key,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime timestamp,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgEnhetId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean erDagligOppfolging,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<LocalDate> gyldigTom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<Tilknytningtype> orgtilknytningType
) {}
