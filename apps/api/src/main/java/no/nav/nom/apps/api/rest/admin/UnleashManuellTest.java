package no.nav.nom.apps.api.rest.admin;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Admin for å teste unleash toggles/strategier.")
@RestController
@RequestMapping("/api/admin/unleash")
@PreAuthorize("hasAuthority('ADMIN')")
public class UnleashManuellTest {
    private final Unleash unleash;

    public UnleashManuellTest(Unleash unleash) {
        this.unleash = unleash;
    }

    @Operation(
        description = "Sjekk om toggle er enablet. Bruker token brukers context",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/sjekkToggle")
    public boolean sendEmailTest(@RequestParam String unleashToggle) {
        return unleash.isEnabled(unleashToggle);
    }
}
