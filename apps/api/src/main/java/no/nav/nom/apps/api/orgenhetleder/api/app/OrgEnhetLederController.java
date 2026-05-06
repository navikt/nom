package no.nav.nom.apps.api.orgenhetleder.api.app;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhetleder.api.app.dto.OpprettOrgEnhetLederRequest;
import no.nav.nom.apps.api.orgenhetleder.api.app.dto.OrgEnhetLederResponse;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.CREATED;

@Slf4j
@Tag(name = "OrgEnhetLedere")
@RestController
@RequestMapping("/app/orgenhetleder")
public class OrgEnhetLederController {
    private final OrgEnhetLederRestService orgEnhetLederRestService;
    private final Unleash unleash;

    public OrgEnhetLederController(
        OrgEnhetLederRestService orgEnhetLederRestService,
        Unleash unleash
    ) {
        this.orgEnhetLederRestService = orgEnhetLederRestService;
        this.unleash = unleash;
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

    @Operation(
        description = "Oppdater leder på OrgEnhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/opprettLeder")
    public ResponseEntity<OrgEnhetLederResponse> opprettLeder(@RequestBody OpprettOrgEnhetLederRequest opprettOrgEnhetLederRequest) {
        if (unleash.isEnabled("nom-api.orgenhet-mastring")) {
            return ResponseEntity.status(CREATED).body(orgEnhetLederRestService.opprettOrgEnhetLeder(opprettOrgEnhetLederRequest));
        } else {
            return ResponseEntity.status(CREATED).body(new OrgEnhetLederResponse(
                "FID_1",
                "X987654",
                new Visningsnavn("Nav", Optional.empty(), "Navesen"),
                "ab123c",
                "Nav Lokalkontor",
                LocalDate.now(),
                null
            ));
        }
    }
}
