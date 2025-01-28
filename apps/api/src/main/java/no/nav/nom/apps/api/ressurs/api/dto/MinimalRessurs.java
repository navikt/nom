package no.nav.nom.apps.api.ressurs.api.dto;


import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record MinimalRessurs(
        
        @Schema(requiredMode = REQUIRED)
        String navident,

        @Schema(requiredMode = REQUIRED)
        String visningsnavn
        
) {
}
