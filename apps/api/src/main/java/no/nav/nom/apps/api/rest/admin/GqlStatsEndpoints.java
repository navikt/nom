package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.querystats.GqlStatConsolidator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@Tag(name = "Admin for å GraphQL statistikk.")
@RestController
@RequestMapping("/api/admin/graphqlstats")
@PreAuthorize("hasAuthority('ADMIN')")
public class GqlStatsEndpoints {

    private final GqlStatConsolidator gqlStatConsolidator;

    public GqlStatsEndpoints(GqlStatConsolidator gqlStatConsolidator) {
        this.gqlStatConsolidator = gqlStatConsolidator;
    }

    @Operation(
        description = "Konsolider usage data for dato",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/consolidateDate")
    public String consolidateDate(@RequestParam LocalDate date) {
        return gqlStatConsolidator.consolidateGQLUsage(date);
    }

    @Operation(
        description = "Konsolider usage data for alle datoer eldre enn 30 dager",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/consolidateAllMinus30Days")
    public void consolidateAllMinus30Days() {
        gqlStatConsolidator.consolidateAllMinus30Days();
    }
}
