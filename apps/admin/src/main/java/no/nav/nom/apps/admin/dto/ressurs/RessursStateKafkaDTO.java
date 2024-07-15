package no.nav.nom.apps.admin.dto.ressurs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Builder(toBuilder = true)
public record RessursStateKafkaDTO (
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String key,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime timestamp,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String personident,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Visningsnavn visningsnavn,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<String> primaryTelefonnummer,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Sektor sektor,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<List<OrgtilknytningKafkaDTO>> orgtilknytninger,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<LocalDate> startdato,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<LocalDate> sluttdato
) { }
