package no.nav.nom.apps.admin.sanitycheck.dto.ressurs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record RessursMedFlereDOL(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgEnhetId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgEnhetNavn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigFom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate gyldigTom
) {

}
