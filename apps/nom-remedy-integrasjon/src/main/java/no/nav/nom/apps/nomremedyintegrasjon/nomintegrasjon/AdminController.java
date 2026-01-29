package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.errorstate.ErrorState;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy.NomIntegrasjonEntity;
import no.nav.nom.contract.nom.eventsource.Event;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

import static no.nav.nom.apps.nomremedyintegrasjon.config.SwaggerConfig.BEARER_TOKEN;

@RestController
@Tag(name = "Admin for manuell opprydding")
@RequestMapping(value = {"/admin"})
public class AdminController {

    private final NomRemedyIntegrasjonService nomRemedyIntegrasjonService;

    public AdminController(NomRemedyIntegrasjonService nomRemedyIntegrasjonService) {
        this.nomRemedyIntegrasjonService = nomRemedyIntegrasjonService;
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
        return ResponseEntity.ok(nomRemedyIntegrasjonService.getNomIntegrasjonEntities());
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
    public ResponseEntity<String> lesOgEksekverRessursState(
        @PathVariable String navident,
        @RequestParam(required = false, defaultValue = "false") boolean isAdmin
    ) {
        nomRemedyIntegrasjonService.lesStatePaaNytt(navident, isAdmin);
        return ResponseEntity.ok("RessursState er lest og eksekvert");
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping(value = "getAllErrorState")
    public ResponseEntity<List<ErrorState>> getAllErrorState() {
        return ResponseEntity.ok(nomRemedyIntegrasjonService.getAllErrorStates());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping(value = "deleteAllErrorStates")
    public ResponseEntity<String> deleteAllErrorStates() {
        nomRemedyIntegrasjonService.deleteAllErrorStates();
        return ResponseEntity.ok("Slettet alle error states");
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping(value = "deleteErrorState/{fid}")
    public ResponseEntity<String> deleteErrorState(@PathVariable String fid) {
        nomRemedyIntegrasjonService.deleteErrorState(fid);
        return ResponseEntity.ok("Slettet error state med id: " + HtmlUtils.htmlEscape(fid));
    }

    @GetMapping(value = "getAllDelayedEventsBlocking")
    public ResponseEntity<List<Event>> getAllDelayedEventsBlocking() {
        List<Event> events = nomRemedyIntegrasjonService.getAllDelayedEventsBlocking();
        return ResponseEntity.ok(events);
    }
}
