package no.nav.nom.apps.admin.dto.ressurs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record RessursHendelseKafkaDTO(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String key,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime timestamp,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Visningsnavn visningsnavn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate startdato,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate sluttdato,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Sektor sektor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RessursHendelseOrgtilknytningKafkaDTO orgtilknytning,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String eventtype
) { }
