package no.nav.nom.apps.api.orgenhet.api.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import no.nav.nom.contract.nom.OrgEnhetsType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Builder(toBuilder = true)
public record OpprettOrgEnhetRequest(
    @NotBlank(message = "Overordnet OrgEnhet ID må være satt")
    @Schema(example = "de456f") String overordnetOrgEnhetId,
    @NotBlank(message = "OrgEnhet må ha navn satt")
    @Schema(example = "Nav Lokal kontor") String navn,
    @NotNull(message = "Gyldig fra og med dato må være satt")
    @Schema(example = "2026-01-31") LocalDate gyldigFom,
    @NotBlank(message = "Leder må være satt for OrgEnheten")
    @Schema(example = "A123456") String lederRessursNavident,
    //@NotBlank(message = "Organisasjonsenheten må tilhøre en RemedyEnhet")
    @Schema(example = "1234") String tilgangsenhetId,
    @NotNull(message = "OrgEnhet må ha en OrgEnhetsType satt")
    @Schema(example = "DIREKTORAT") OrgEnhetsType orgEnhetsType
) {}
