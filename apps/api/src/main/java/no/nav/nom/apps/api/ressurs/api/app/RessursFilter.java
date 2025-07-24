package no.nav.nom.apps.api.ressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record RessursFilter(
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    Sektor sektor,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    LocalDate sluttdato
) {
}
