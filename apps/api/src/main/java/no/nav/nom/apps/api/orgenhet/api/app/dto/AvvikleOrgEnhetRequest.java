package no.nav.nom.apps.api.orgenhet.api.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record AvvikleOrgEnhetRequest(
    @Schema(example = "2026-12-31") LocalDate gyldigTom
) {
}
