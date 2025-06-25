package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.errorstate.ErrorState;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy.NomIntegrasjonEntity;
import no.nav.nom.contract.nom.eventsource.Event;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.nomremedyintegrasjon.config.SecurityConfig.NAVIDENT_CLAIM_NAME;
import static no.nav.nom.apps.nomremedyintegrasjon.config.SwaggerConfig.BEARER_TOKEN;

@RestController
@Tag(name = "Admin for manuell opprydding")
@RequestMapping(value = {"/admin"})
public class AdminController {

    private final NomRemedyIntegrasjonServiceOld nomRemedyIntegrasjonServiceOld;
    private final NomRemedyIntegrasjonServiceNew nomRemedyIntegrasjonServiceNew;
    private final Unleash unleash;

    public AdminController(NomRemedyIntegrasjonServiceOld nomRemedyIntegrasjonServiceOld,
                           NomRemedyIntegrasjonServiceNew nomRemedyIntegrasjonServiceNew, Unleash unleash
    ) {
        this.nomRemedyIntegrasjonServiceOld = nomRemedyIntegrasjonServiceOld;
        this.nomRemedyIntegrasjonServiceNew = nomRemedyIntegrasjonServiceNew;
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
    public ResponseEntity<String> slettFraRemedy(
        @Parameter(description = "Sluttdato for ressurser som skal slettes")
        @RequestParam LocalDate sluttdato
    ) {
        if (sluttdato.isAfter(LocalDate.now().minusDays(1))) {
            return ResponseEntity.badRequest().body("Ikke mulig å slette aktive ressurser fra remedy");
        }

        nomRemedyIntegrasjonServiceOld.findRessurserWithSluttdatoAndMarkForDeletion(sluttdato, getAuthenticatedNavIdent());

        return ResponseEntity.ok("Fjernet ressurser med spesifisert sluttdato");
    }


    @Operation(
        description = "Lister alle bestillinger sendt til remedy",
        security = @SecurityRequirement(name = BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
        }
    )
    @GetMapping(value = "/bestillinger")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<NomIntegrasjonEntity>> getBestillinger() {
        return ResponseEntity.ok(nomRemedyIntegrasjonServiceOld.getNomIntegrasjonEntities());
    }

    @Operation(
        description = "Les og eksekver ressurserState til remedy",
        security = @SecurityRequirement(name = BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
        }
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(value = "lesOgEksekverRessursState/{navident}")
    public ResponseEntity<String> lesOgEksekverRessursState(@PathVariable String navident) {
        if (unleash.isEnabled("nom-remedy.ny-remedy-integrasjon")) {
            nomRemedyIntegrasjonServiceNew.lesStatePaaNytt(navident);
        } else {
            nomRemedyIntegrasjonServiceOld.lesStatePaaNytt(getAuthenticatedNavIdent(), navident);
        }
        return ResponseEntity.ok("RessursState er lest og eksekvert");
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping(value = "getAllErrorState")
    public ResponseEntity<List<ErrorState>> getAllErrorState() {
        return ResponseEntity.ok(nomRemedyIntegrasjonServiceNew.getAllErrorStates());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping(value = "deleteAllErrorStates")
    public ResponseEntity<String> deleteAllErrorStates() {
        nomRemedyIntegrasjonServiceNew.deleteAllErrorStates();
        return ResponseEntity.ok("Slettet alle error states");
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping(value = "deleteErrorState/{fid}")
    public ResponseEntity<String> deleteErrorState(@PathVariable String fid) {
        nomRemedyIntegrasjonServiceNew.deleteErrorState(fid);
        return ResponseEntity.ok("Slettet error state med id: " + fid);
    }

    @GetMapping(value = "getAllDelayStates")
    public Flux<Event> getAllDelayStates() {
        return nomRemedyIntegrasjonServiceNew.getAllDelayedEvents();
    }

    private static String getAuthenticatedNavIdent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Optional.ofNullable(jwt.getClaimAsString(NAVIDENT_CLAIM_NAME)).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, String.format("Couldn't find %s in Claims", NAVIDENT_CLAIM_NAME))
        );
    }
}
