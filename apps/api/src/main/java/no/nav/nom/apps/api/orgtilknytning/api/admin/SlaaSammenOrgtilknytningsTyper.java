package no.nav.nom.apps.api.orgtilknytning.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningTypeService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Admin for korrigeringer")
@RestController
@RequestMapping("/api/admin/SlaaSammenOrgtilknytningsTyperUforutsettBehovOgVikarTilMidlertidig")
@PreAuthorize("hasAuthority('ADMIN')")
public class SlaaSammenOrgtilknytningsTyper {
    private final OrgtilknytningTypeService orgtilknytningTypeService;

    public SlaaSammenOrgtilknytningsTyper(OrgtilknytningTypeService orgtilknytningTypeService) {
        this.orgtilknytningTypeService = orgtilknytningTypeService;
    }

    @Operation(
        description = "Slå sammen tilknytningstype Uforutsett behov og Vikar til Midlertidig",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping
    public void slaaSammenOrgtilknytningsTyperUforutsettBehovOgVikarTilMidlertidig() {
        replaceUforutsettbehovOgVikarOrgtilknytningTypeTilMidlertidig();
    }

    @Async
    public void replaceUforutsettbehovOgVikarOrgtilknytningTypeTilMidlertidig() {
        orgtilknytningTypeService.replaceUforutsettbehovOgVikarOrgtilknytningTypeTilMidlertidig();
    }

}
