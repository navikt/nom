package no.nav.nom.apps.admin.sanitycheck.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.admin.SwaggerConfig;
import no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning.OrgtilknytningMedOrgEnhet;
import no.nav.nom.apps.admin.sanitycheck.service.OrgtilknytningSanityCheckService;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin for å kjøre sanity check på Orgtilknytninger")
@RestController
@RequestMapping("SanityCheck/orgtilknytning")
@PreAuthorize("hasAuthority('ADMIN')")
public class OrgtilknytningSanityCheckController {

    private final OrgtilknytningSanityCheckService orgtilknytningSanityCheckService;

    public OrgtilknytningSanityCheckController(OrgtilknytningSanityCheckService orgtilknytningSanityCheckService) {
        this.orgtilknytningSanityCheckService = orgtilknytningSanityCheckService;
    }


    @Operation(
        description = "List tilknytninger med gyldighet utover enhetens gyldighetsperiode",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listTilkoblingerUtoverOrgEnhetGyldighetPeriode")
    public ResponseEntity<List<OrgtilknytningMedOrgEnhet>> listTilkoblingerUtoverOgEnhetGyldighetPeriode() {
        return ResponseEntity.ok(orgtilknytningSanityCheckService.orgtilknytningerUtoverEnhetGyldighetsPeriode());
    }

    @Operation(
        description = "List orgtilknytninger med negativ periode (gyldigTom < gyldigFom)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/orgtilknytningNegativPeriode")
    public ResponseEntity<List<OrgtilknytningState>> orgtilknytningNegativPeriode() {
        return ResponseEntity.ok(orgtilknytningSanityCheckService.orgtilknytningNegativPeriode());
    }

    @Operation(
        description = "List orgtilknytninger med flere dagligOppfolging i current periode (gyldigFom < now < gyldigTom)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/orgtilknytningMedFlereAktivDagligOppfolging")
    public ResponseEntity<List<OrgtilknytningState>> orgtilknytningMedFlereAktivDagligOppfolging() {
        // Group by navident for current period and dagligOppfolging
        return ResponseEntity.ok(orgtilknytningSanityCheckService.orgtilknytningerMedFlereDOLSammePeriode());
    }
}
