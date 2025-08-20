package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Orgtilknytning(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgEnhetId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate gyldigTom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean dagligOppfolging
        
) {
}
