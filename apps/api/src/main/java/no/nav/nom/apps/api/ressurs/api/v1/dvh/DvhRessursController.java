package no.nav.nom.apps.api.ressurs.api.v1.dvh;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "Datavarehus")
@RestController
@RequestMapping("/api/v1/dvh")
public class DvhRessursController {
    private final DvhRessursRestService restService;

    public DvhRessursController(DvhRessursRestService restService) {
        this.restService = restService;
    }

    @Operation(
        description = "Hent alle ressurser med orgtilknytninger som er mastret i NOM",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/nom-mastered-ressurser")
    public ResponseEntity<List<DvhRessursDTO>> nomMasteredRessurser() {
        return ResponseEntity.ok(restService.nomMasteredRessurser());
    }

    @Operation(
        description = "Hent alle Orgenheter med tillegsinfo",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/orgenheter")
    public ResponseEntity<List<DvhOrgenhetDTO>> orgenheter() {
        return ResponseEntity.ok(restService.orgenheter());
    }
}
