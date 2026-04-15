package no.nav.nom.apps.api.orgtilknytning.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;

@Builder(toBuilder = true)
public record EndreOrgtilknytningRequest(
    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String fid,
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDate gyldigFom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    Sektor sektor,
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    Boolean erDagligOppfolging,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    LocalDate gyldigTom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    Tilknytningtype tilknytningtype,
    @NotBlank(message = "Hoved fysisk brukersted kan ikke være tom")
    @Schema(example = "2700", requiredMode = Schema.RequiredMode.REQUIRED)
    String primaryFysiskBrukerstedNummer,
    @Schema(example = "[\"2700\", \"2900\"]")
    Set<String> optionalFysiskBrukersteder
) {
}
