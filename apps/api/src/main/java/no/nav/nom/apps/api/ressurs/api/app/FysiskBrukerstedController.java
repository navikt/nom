package no.nav.nom.apps.api.ressurs.api.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.ressurs.FysiskBrukerstedService;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "Fysiske Brukersteder")
@RestController
@RequestMapping(value = {"/app/fysiskeBrukersteder"})
public class FysiskBrukerstedController {

    private final FysiskBrukerstedService fysiskBrukerstedService;

    public FysiskBrukerstedController(FysiskBrukerstedService fysiskBrukerstedService) {
        this.fysiskBrukerstedService = fysiskBrukerstedService;
    }

    @GetMapping
    @Operation(
        description = "Henter alle fysiske brukersteder fra MainManager",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK")
    })
    public ResponseEntity<List<FysiskBrukersted>> getAllFysiskeBrukersteder() {
        return ResponseEntity.ok(fysiskBrukerstedService.getAllFysiskBrukersted().values().stream().toList());
    }
}
