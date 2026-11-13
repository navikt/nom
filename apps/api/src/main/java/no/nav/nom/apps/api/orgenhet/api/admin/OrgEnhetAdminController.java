package no.nav.nom.apps.api.orgenhet.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin for OrgEnhet")
@RestController
@RequestMapping(value = {"/api/admin/orgenhet"})
@PreAuthorize("hasAuthority('ADMIN')")
public class OrgEnhetAdminController {
    private final OrgenhetService orgEnhetService;
    private final SetteGyldigTomPaaOrgEnheterUtenOrganisering setteGyldigTomPaaOrgEnheterUtenOrganisering;

    public OrgEnhetAdminController(OrgenhetService orgEnhetService, SetteGyldigTomPaaOrgEnheterUtenOrganisering setteGyldigTomPaaOrgEnheterUtenOrganisering) {
        this.orgEnhetService = orgEnhetService;
        this.setteGyldigTomPaaOrgEnheterUtenOrganisering = setteGyldigTomPaaOrgEnheterUtenOrganisering;
    }

    @PostMapping("/sammenstill")
    @Operation(
        description = "Manuell kjøring av orgEnhet samstemming av gyldigTom mot Organisering",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public void sammenstillOrgEnhetGyldigTom() {
        orgEnhetService.alignOrgEnhetGyldigTomWithOrganisering();
    }

    @PostMapping("/gyldig-tom-historiske-uten-organisering")
    @Operation(
        description = "Skript som setter gyldig til og med dato på org enhet som aldri har vært organisert",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<String> gyldigTomHistoriskeUtenOrganisering(@RequestParam boolean dryRun) {
        return ResponseEntity.ok(setteGyldigTomPaaOrgEnheterUtenOrganisering.settGyldigTomPaaOrgEnheterUtenOrganisering(dryRun));
    }
}
