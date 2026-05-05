package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.common.speiling.SpeilingService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin for å trigge speiling av data fra orgtilknytning til ressurs")
@RestController
@RequestMapping("/api/admin/speilingFraOrgtilknytningTilRessurs")
@PreAuthorize("hasAuthority('ADMIN') || hasAuthority('LOCAL_NOM_API')")
@Slf4j
public class ApiForSpeileDataFraOrgtilknytningTilRessurs {
    private final SpeilingService speilingService;

    public ApiForSpeileDataFraOrgtilknytningTilRessurs(SpeilingService speilingService) {
        this.speilingService = speilingService;
    }

    @Operation(
        description = "Speil tilknytningstype fra orgtilknytning til ressurs (nattlige jobben i SpeilingService)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/speilTilknytningtype", produces="application/json")
    public void speilTilknytningtype() {
        speilTilknytningtypeAsync();
    }

    @Async
    public void speilTilknytningtypeAsync() {
        speilingService.speilTilknytningtypeOrgtilknytning();
    }

    @Operation(
        description = "Speil sektor fra orgtilknytning til ressurs (nattlige jobben i SpeilingService)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/speilSektor", produces="application/json")
    public void speilSektor() {
        speilSektorAsync();
    }

    @Async
    public void speilSektorAsync() {
        speilingService.speilSektorOrgtilknytning();
    }

    @Operation(
        description = "Speil fysisk brukersteder fra orgtilknytning til ressurs",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/speilFysiskBrukersteder")
    public void speilFysiskBrukersteder() {
        speilFysiskBrukerstederAsync();
    }

    @Async
    public void speilFysiskBrukerstederAsync() {
        speilingService.speilBrukerstederOrgtilknytning();
    }
}
