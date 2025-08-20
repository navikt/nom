package no.nav.nom.apps.admin.dto.ressurs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record RessursHendelseOrgtilknytningKafkaDTO(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgEnhetId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate gyldigTom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean dagligOppfolging
) {
}
