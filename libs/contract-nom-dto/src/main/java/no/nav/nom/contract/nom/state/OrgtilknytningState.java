package no.nav.nom.contract.nom.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrgtilknytningState(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fid,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgEnhetId,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Sektor sektor,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean erDagligOppfolging,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate gyldigTom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Tilknytningtype orgtilknytningType,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String masseflyttetFraFid,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String sistEndretAv
) {}
