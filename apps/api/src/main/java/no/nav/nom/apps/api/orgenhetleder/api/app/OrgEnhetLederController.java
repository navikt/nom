package no.nav.nom.apps.api.orgenhetleder.api.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "OrgEnhetLedere")
@RestController
@RequestMapping("/app/orgenhetleder")
public class OrgEnhetLederController {
    private final OrgEnhetLederRestService orgEnhetLederRestService;

    public OrgEnhetLederController(
        OrgEnhetLederRestService orgEnhetLederRestService
    ) {
        this.orgEnhetLederRestService = orgEnhetLederRestService;
    }

    @GetMapping("/by-navident/{navident}")
    @Operation(
        description = "Henter alle orgenheter en er leder for med navident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<List<OrgEnhetLederResponse>> getLederByNavident(@PathVariable String navident) {
        return ResponseEntity.ok(orgEnhetLederRestService.getLederByNavident(navident));
    }

    @GetMapping("/byOrgEnhetId/{orgenhetId}")
    @Operation(
        description = "Henter alle periodiserte ledere for gitt orgenhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<List<OrgEnhetLederResponse>> getLedereByOrgenhetId(@PathVariable String orgenhetId) {
        return ResponseEntity.ok(orgEnhetLederRestService.getLederByOrgEnhetId(orgenhetId));
    }

    @GetMapping("/{orgenhetId}")
    @Operation(
        description = "Hent aktiv leder for orgenhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    public ResponseEntity<OrgEnhetLederResponse> getLederByOrgEnhetId(@PathVariable String orgenhetId) {
        var orgEnhetLederResponse = orgEnhetLederRestService.getActivLederForOrgEnhet(orgenhetId);
        return orgEnhetLederResponse.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
