package no.nav.nom.contract.nom.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;
import java.util.Optional;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrgtilknytningState(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgEnhetId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean erDagligOppfolging,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<LocalDate> gyldigTom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<Tilknytningtype> orgtilknytningType
) {}
