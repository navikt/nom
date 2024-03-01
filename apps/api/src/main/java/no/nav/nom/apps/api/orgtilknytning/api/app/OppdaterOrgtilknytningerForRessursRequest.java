package no.nav.nom.apps.api.orgtilknytning.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record OppdaterOrgtilknytningerForRessursRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<LeggTilOrgtilknytningRequest> nyeOrgtilknytninger,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<EndreOrgtilknytningRequest> endredeOrgtilknytninger,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Boolean setRessursSluttdato,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Boolean setRessursStartdato
) { }
