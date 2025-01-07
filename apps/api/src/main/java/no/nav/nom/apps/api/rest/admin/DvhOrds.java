package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.common.SwaggerConfig;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin for å nedlasting fra DVH ORDS")
@RestController
@RequestMapping("/api/admin/dvhOrds")
@PreAuthorize("hasAuthority('ADMIN') || hasAuthority('LOCAL_NOM_API')")
@Slf4j
public class DvhOrds {
    private final DvhOrdsService dvhOrdsService;

    public DvhOrds(DvhOrdsService dvhOrdsService) {
        this.dvhOrdsService = dvhOrdsService;
    }

    @Operation(
            description = "Hent enheter",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
            {
                    @ApiResponse(responseCode = "200", description = "Success")
            }
    )
    @GetMapping(value = "/enheter", produces="application/json")
    public String enheter(@RequestParam(defaultValue ="1") int numRows, @RequestParam(required = false) String ordsFilter) {
        if (ordsFilter != null) {
            return dvhOrdsService.getOrgenheterJson(ordsFilter).toString();
        }
        return dvhOrdsService.getOrgenheterJson(numRows).toString();
    }

    @Operation(
            description = "Hent organisering og ledere",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
            {
                    @ApiResponse(responseCode = "200", description = "Success")
            }
    )
    @GetMapping(value = "/organiseringOgLedere", produces="application/json")
    public String organiseringOgLedere(@RequestParam(defaultValue ="1") int numRows, @RequestParam(required = false) String ordsFilter) {
        if (ordsFilter != null) {
            return dvhOrdsService.getOrganiseringOgLedereJson(ordsFilter).toString();
        }
        return dvhOrdsService.getOrganiseringOgLedereJson(numRows).toString();
    }

    @Operation(
            description = "Hent ressurser",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
            {
                    @ApiResponse(responseCode = "200", description = "Success")
            }
    )
    @GetMapping(value = "/ressurser", produces="application/json")
    public String ressurser(@RequestParam(defaultValue ="1") int numRows, @RequestParam(required = false) String ordsFilter) {
        if (ordsFilter != null) {
            return dvhOrdsService.getRessurserJson(ordsFilter).toString();
        }
        return dvhOrdsService.getRessurserJson(numRows).toString();
    }

    @Operation(
            description = "Hent statlige ressurser tilknytninger",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
            {
                    @ApiResponse(responseCode = "200", description = "Success")
            }
    )
    @GetMapping(value = "/statligeRessurserTilknytninger", produces="application/json")
    public String statligeRessurserTilknytninger(@RequestParam(defaultValue ="1") int numRows, @RequestParam(required = false) String ordsFilter) {
        if (ordsFilter != null) {
            return dvhOrdsService.getStatligeTilknytningerJson(ordsFilter).toString();
        }
        return dvhOrdsService.getStatligeTilknytningerJson(numRows).toString();
    }

    @Operation(
        description = "Hent statlige ressurser tilknytninger - paginert",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/statligeRessurserTilknytningerPaginated", produces="application/json")
    public String statligeRessurserTilknytningerPaginated(@RequestParam(defaultValue ="0") int offset, @RequestParam(defaultValue ="100000") int limit) {
        return dvhOrdsService.getStatligeTilknytningerJsonPaginated(offset, limit).toString();
    }

    @Operation(
            description = "Hent kommunale ressurser",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
            {
                    @ApiResponse(responseCode = "200", description = "Success")
            }
    )
    @GetMapping(value = "/kommunaleRessurser", produces="application/json")
    public String kommunaleRessurser(@RequestParam(defaultValue ="1") int numRows, @RequestParam(required = false) String ordsFilter) {
        if (ordsFilter != null) {
            return dvhOrdsService.getKommunaleRessurserJson(ordsFilter).toString();
        }
        return dvhOrdsService.getKommunaleRessurserJson(numRows).toString();
    }

    @Operation(
            description = "Hent eksterne ressurser tilknytninger",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
            {
                    @ApiResponse(responseCode = "200", description = "Success")
            }
    )
    @GetMapping(value = "/eksterneRessurserTilknytninger", produces="application/json")
    public String eksterneRessurserTilknytninger(@RequestParam(defaultValue ="1") int numRows, @RequestParam(required = false) String ordsFilter) {
        if (ordsFilter != null) {
            return dvhOrdsService.getEksterneTilknytningerJson(ordsFilter).toString();
        }
        return dvhOrdsService.getEksterneTilknytningerJson(numRows).toString();
    }

    @Operation(
            description = "Hent telefoner",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
            {
                    @ApiResponse(responseCode = "200", description = "Success")
            }
    )
    @GetMapping(value = "/telefoner", produces="application/json")
    public String telefoner(@RequestParam(defaultValue ="1") int numRows, @RequestParam(required = false) String ordsFilter) {
        if (ordsFilter != null) {
            return dvhOrdsService.getTlfJson(ordsFilter).toString();
        }
        return dvhOrdsService.getTlfJson(numRows).toString();
    }

    @Operation(
        description = "Hent gamle nom-ressurser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/nom-ressurser", produces="application/json")
    public String nomRessurser(@RequestParam(defaultValue ="1") int numRows, @RequestParam(required = false) String ordsFilter) {
        if (ordsFilter != null) {
            return dvhOrdsService.getNomRessurserJson(ordsFilter).toString();
        }
        return dvhOrdsService.getNomRessurserJson(numRows).toString();
    }
}
