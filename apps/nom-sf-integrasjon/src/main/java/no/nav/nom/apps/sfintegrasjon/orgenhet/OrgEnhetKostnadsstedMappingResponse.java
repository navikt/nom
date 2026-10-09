package no.nav.nom.apps.sfintegrasjon.orgenhet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record OrgEnhetKostnadsstedMappingResponse(
    @Schema(requiredMode = REQUIRED)
    String kostnadsstedId,
    @Schema(requiredMode = REQUIRED)
    String orgenhetId,
    @Schema(requiredMode = REQUIRED)
    LocalDate gyldigFom,
    @Schema(requiredMode = REQUIRED)
    LocalDate gyldigTom
) {
}
