package no.nav.nom.apps.nomremedyintegrasjon.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static no.nav.nom.apps.nomremedyintegrasjon.config.SwaggerConfig.BEARER_TOKEN;

@RestController
@RequestMapping(value = {"/api/fysiskebrukersteder"})
public class FysiskBrukerstedController {

    private final FysiskBrukerstedReadOnlyRepository fysiskBrukerstedReadOnlyRepository;

    public FysiskBrukerstedController(FysiskBrukerstedReadOnlyRepository fysiskBrukerstedReadOnlyRepository) {
        this.fysiskBrukerstedReadOnlyRepository = fysiskBrukerstedReadOnlyRepository;
    }

    @Operation(
        description = "Leter opp fysiske brukersteder",
        security = @SecurityRequirement(name = BEARER_TOKEN)
    )
    @GetMapping
    public ResponseEntity<List<FysiskBrukersted>> getFysiskeBrukersteder(@RequestParam(required = false) List<String> brukerstedNrs) {

        if (brukerstedNrs != null) {
            return ResponseEntity.ok(fysiskBrukerstedReadOnlyRepository.findByBrukerstedNrIn(brukerstedNrs));
        }

        return ResponseEntity.ok(fysiskBrukerstedReadOnlyRepository.findAll());
    }
}
