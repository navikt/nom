package no.nav.nom.apps.api.orgenhet.api.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record OpprettOrgEnhetRequest(
    @Schema(example = "de456f") String overordnetOrgEnhetId,
    @Schema(example = "Nav Lokal kontor") String navn,
    @Schema(example = "2026-01-31") LocalDate gyldigFom,
    @Schema(example = "A123456") String lederRessursNavident,
    @Schema(example = "1234") String firesiffer,
    @Schema(example = "DIREKTORAT") OrgEnhetsType orgEnhetsType
) {}
