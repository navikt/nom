package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static no.nav.nom.apps.nomremedyintegrasjon.config.SecurityConfig.NAVIDENT_CLAIM_NAME;
import static no.nav.nom.apps.nomremedyintegrasjon.config.SwaggerConfig.BEARER_TOKEN;

@RestController
@Tag(name = "Admin for manuell opprydding")
@RequestMapping(value = {"/admin"})
public class AdminController {

    private final NomIntegrasjonService nomIntegrasjonService;
    private final Unleash unleash;

    public AdminController(NomIntegrasjonService nomIntegrasjonService, Unleash unleash) {
        this.nomIntegrasjonService = nomIntegrasjonService;
        this.unleash = unleash;
    }

    @Operation(
        description = "Leter opp eksterne ressurser med angitt sluttdato og sletter de fra remedy",
        security = @SecurityRequirement(name = BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
        }
    )
    @PostMapping(value = "/slettFraRemedy")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> yoloBrolo(
        @Parameter(description = "Sluttdato for ressurser som skal slettes")
        @RequestParam LocalDate sluttdato
    ) {
        if (!unleash.isEnabled("nom-remedy-integrasjon.delete_avsluttede_ressurser")) {
            return ResponseEntity.status(403).body("Feature er ikke aktivert ennå");
        }

        if (sluttdato.isAfter(LocalDate.now().minusDays(1))) {
            return ResponseEntity.badRequest().body("Ikke mulig å slette aktive ressurser fra remedy");
        }

        nomIntegrasjonService.findRessurserWithSluttdatoAndMarkForDeletion(sluttdato, getAuthenticatedNavIdent());

        return ResponseEntity.ok("Fjernet ressurser med spesifisert sluttdato");
    }

    public static String getAuthenticatedNavIdent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Optional.ofNullable(jwt.getClaimAsString(NAVIDENT_CLAIM_NAME)).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, String.format("Couldn't find %s in Claims", NAVIDENT_CLAIM_NAME))
        );
    }
}
