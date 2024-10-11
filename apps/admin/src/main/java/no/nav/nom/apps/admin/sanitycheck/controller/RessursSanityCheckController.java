package no.nav.nom.apps.admin.sanitycheck.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.admin.SwaggerConfig;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursOrgtilknytningInfo;
import no.nav.nom.apps.admin.sanitycheck.service.RessursSanityCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin for å kjøre sanity check på Ressurs")
@RestController
@RequestMapping("SanityCheck/ressurs")
@PreAuthorize("hasAuthority('ADMIN')")
public class RessursSanityCheckController {

    private final RessursSanityCheckService ressursSanityCheckService;

    public RessursSanityCheckController(RessursSanityCheckService ressursSanityCheckService) {
        this.ressursSanityCheckService = ressursSanityCheckService;
    }

    @Operation(
        description = "Sjekk aktive ressurser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/ressurser")
    public ResponseEntity<RessursOrgtilknytningInfo> dataSanityCheckRessurser() {
        return ResponseEntity.ok(ressursSanityCheckService.aktiveRessurser());
    }
}
