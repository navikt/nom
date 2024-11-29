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
        description = "Test epost sending",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/eksterneRessurser", produces="application/json")
    public String eksterneRessurser(@RequestParam(defaultValue ="1") int numRows, @RequestParam(required = false) String ordsFilter) {
        if (ordsFilter != null) {
            return dvhOrdsService.getEksterneRessurserJson(ordsFilter).toString();
        }
        return dvhOrdsService.getEksterneRessurserJson(numRows).toString();
    }
}
