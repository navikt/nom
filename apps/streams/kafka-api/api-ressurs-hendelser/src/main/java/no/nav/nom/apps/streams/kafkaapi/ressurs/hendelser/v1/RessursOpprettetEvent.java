package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.util.Optional;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RessursOpprettetEvent(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Visningsnavn visningsnavn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate startdato,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<LocalDate> sluttdato,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Sektor sektor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Orgtilknytning orgtilknytning
) {
}
