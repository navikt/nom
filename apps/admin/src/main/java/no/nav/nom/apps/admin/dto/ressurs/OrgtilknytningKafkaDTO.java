package no.nav.nom.apps.admin.dto.ressurs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;
import java.util.Optional;

@Builder(toBuilder = true)
public record OrgtilknytningKafkaDTO(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgenhetId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate gyldigTom,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean dagligOppfolging,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Sektor sektor,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Tilknytningtype tilknytningType
) {
}
