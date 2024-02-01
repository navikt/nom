package no.nav.nom.apps.api.ressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;
import java.util.Optional;

@Builder(toBuilder = true)
public record RessursFilter(
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    Optional<Sektor> sektor,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    Optional<LocalDate> sluttdato
) {
}
