package no.nav.nom.apps.admin.sanitycheck.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.admin.SwaggerConfig;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.EnheterOgOrganisering;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.OrgEnhetStartOgSluttdato;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.OrgEnhetOrganisering;
import no.nav.nom.apps.admin.sanitycheck.service.OrgEnhetSanityCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin for å kjøre sanity check på OrgEnhet")
@RestController
@RequestMapping("SanityCheck/orgenhet")
@PreAuthorize("hasAuthority('ADMIN')")
public class OrgEnhetSanityCheckController {

    private final OrgEnhetSanityCheckService orgEnhetSanityCheckService;

    public OrgEnhetSanityCheckController(OrgEnhetSanityCheckService orgEnhetSanityCheckService) {
        this.orgEnhetSanityCheckService = orgEnhetSanityCheckService;
    }


    @Operation(
        description = "Sjekk enheter antall og dato",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/enheter")
    public ResponseEntity<OrgEnhetStartOgSluttdato> dataSanityCheckEnheter() {
        return ResponseEntity.ok(orgEnhetSanityCheckService.sjekkOrgEnhetStartSluttDato());
    }

    @Operation(
            description = "Sjekk aktive enheter og organiseringer",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
            {
                    @ApiResponse(responseCode = "200", description = "Success")
            }
    )
    @GetMapping(value = "/enheterOgOrganisering")
    public ResponseEntity<EnheterOgOrganisering> dataSanityCheckEnheterOgOrganisering() {
        return ResponseEntity.ok(orgEnhetSanityCheckService.sjekkAktiveEnheterOgOrganisering());
    }

    @Operation(
            description = "List aktive enheter Uten over-enhet",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
            {
                    @ApiResponse(responseCode = "200", description = "Success")
            }
    )
    @GetMapping(value = "/listEnheterUtenOverEnheter")
    public ResponseEntity<List<OrgEnhetOrganisering>> listEnheterUtenOverEnheter() {
        return ResponseEntity.ok(orgEnhetSanityCheckService.enhetUtenOverEnhet());
    }

    @Operation(
            description = "List aktive enheter Uten over-enhet men med under-enheter",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
            {
                    @ApiResponse(responseCode = "200", description = "Success")
            }
    )
    @GetMapping(value = "/listEnheterUtenOverEnheterMedUnderEnheter")
    public ResponseEntity<List<OrgEnhetOrganisering>> listEnheterUtenOverEnheterMedUnderEnheter() {
        return ResponseEntity.ok(orgEnhetSanityCheckService.enhetUtenOverEnhetMedUnderEnhet());
    }
}
