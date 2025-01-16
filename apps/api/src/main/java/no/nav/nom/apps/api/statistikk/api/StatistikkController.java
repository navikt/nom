package no.nav.nom.apps.api.statistikk.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Statistikk")
@RestController
@RequestMapping("/app/statistikk")
public class StatistikkController {

    private final StatistikkRestService statistikkRestService;

    public StatistikkController(StatistikkRestService statistikkRestService) {
        this.statistikkRestService = statistikkRestService;
    }

    @GetMapping
    @Operation(
        description = "Hent global statistikk",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "500", description = "Feil ved henting av statistikk")
        }
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<GlobalStatistikkResponse> hentGlobalStatistikk() {
        return ResponseEntity.ok(statistikkRestService.hentGlobalStatistikk());
    }

    @GetMapping("/{orgenhetId}")
    @Operation(
        description = "Hent statistikk for orgenhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "500", description = "Feil ved henting av statistikk")
        }
    )
    public ResponseEntity<OrgEnhetStatistikkResponse> hentOrgEnhetStatistikk(@PathVariable String orgenhetId) {
        return ResponseEntity.ok(statistikkRestService.hentStatistikkForOrgEnhet(orgenhetId));
    }
}
