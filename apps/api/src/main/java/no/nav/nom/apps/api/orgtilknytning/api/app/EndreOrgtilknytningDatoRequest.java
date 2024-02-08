package no.nav.nom.apps.api.orgtilknytning.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Optional;

@Builder(toBuilder = true)
public record EndreOrgtilknytningDatoRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<LocalDate> gyldigTom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) Optional<Boolean> setRessursSluttdato
) {}

