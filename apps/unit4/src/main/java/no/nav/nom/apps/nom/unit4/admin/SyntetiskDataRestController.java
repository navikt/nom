package no.nav.nom.apps.nom.unit4.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.nom.unit4.common.SwaggerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin for å adminsitrere syntetisk test data")
@RestController
@Profile("!prod")
@RequestMapping("/api/v1/test/syntetisk")
public class SyntetiskDataRestController {
    private final Logger log = LoggerFactory.getLogger(SyntetiskDataRestController.class);


    public SyntetiskDataRestController() {
    }

    @Operation(
        description = "Slett ressurs",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success")
    })
    @GetMapping(value = "/slettRessurs")
    public void slettRessurser(@PathVariable String ressursId) {
        // Slett Unit4 Employee med tilhørende Employments

    }
}
