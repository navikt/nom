package no.nav.nom.apps.admin.sanitycheck.dto.ressurs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;
import java.util.List;

@Builder
public record RobotRessurs(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String visningsnavn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Sektor sektor,
        LocalDate startdato,
        LocalDate sluttdato,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RobotOrgtilknytning> orgtilknytninger
) {
    @Builder
    public record RobotOrgtilknytning(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgenhetId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgenhetNavn,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom,
            LocalDate gyldigTom
    ) {}
}
