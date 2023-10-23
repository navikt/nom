package no.nav.nom.apps.api.orgenhet.api.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;
import java.util.List;

public record OrgEnhet(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String agressoId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgNiv,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String agressoOrgenhetType,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navn,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate gyldigTom,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) NomNivaa nomNivaa,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) OrgEnhetsType orgEnhetsType,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String remedyEnhetId,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String overordnetEnhetId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> underordnedeOrgEnhetId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RessursState> ledere,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<OrgtilknytningState> ressursKoblinger
) { }
