package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhDuplikatAgressoIdAnalyse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin for å analysere litt rundt duplikat AgressoId i DVH last.")
@RestController
@RequestMapping("/api/admin/DvhDuplikatAgressoIdAnalyse")
@PreAuthorize("hasAuthority('ADMIN')")
public class DvhDuplikatAgressoIdAnalyseController {
    DvhDuplikatAgressoIdAnalyse dvhDuplikatAgressoIdAnalyse;

    @Operation(
        description = "List duplikate AgressoIder og duplikate agressoId+Nivå",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/duplikatAnalyse")
    public String duplikatAnalyse() {
        return dvhDuplikatAgressoIdAnalyse.analyserDvhOrgenheterOgStateEnheter();
    }
}
