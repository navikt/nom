package no.nav.nom.apps.api.orgenhet.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Admin for OrgEnhet")
@RestController
@RequestMapping(value = {"/api/admin/orgenhet"})
@PreAuthorize("hasAuthority('ADMIN')")
public class OrgEnhetAdminController {
    private final OrgenhetService orgEnhetService;

    public OrgEnhetAdminController(OrgenhetService orgEnhetService) {
        this.orgEnhetService = orgEnhetService;
    }

    @PostMapping("/sammenstill")
    @Operation(
        description = "Manuell kjøring av orgEnhet samstemming av gyldigTom mot Organisering",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public void sammenstillOrgEnhetGyldigTom() {
        orgEnhetService.alignOrgEnhetGyldigTomWithOrganisering();
    }
}
