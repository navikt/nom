package no.nav.nom.apps.api.ressurs.api.v1.dvh;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

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
        description = "Hent alle ressurser som er mastret i NOM",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/nom-mastered-ressurser")
    public ResponseEntity<List<DvhRessursDTO>> nomMasteredRessurser() {
        return ResponseEntity.ok(restService.nomMasteredRessurser());
    }
}
