package no.nav.nom.apps.api.ressurs.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.masterdata.NomMasteredService;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Admin for å migrering til NOM som master av eksterne og kommunale ressurser")
@RestController
@RequestMapping("/api/admin/migrering")
@PreAuthorize("hasAuthority('ADMIN')")
public class MigreringVedLanseringMastringKommunaleOgEksterne {

    private final RessursStateRepository ressursStateRepository;
    private final NomMastered nomMastered;

    public MigreringVedLanseringMastringKommunaleOgEksterne(RessursStateRepository ressursStateRepository, NomMastered nomMastered) {
        this.ressursStateRepository = ressursStateRepository;
        this.nomMastered = nomMastered;
    }


    @Operation(
        description = "Marker alle kommunale ressurser som mastret i NOM",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    @PostMapping(value = "/markerKommunaleSomMastret")
    public ResponseEntity<String> markerKommunaleSomMastret(
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm
    ) {
        return ResponseEntity.ok(markerKommunaleSomMastret("CONFIRM".equalsIgnoreCase(confirm)));
    }

    protected String markerKommunaleSomMastret(boolean commit) {
        var ikkeMastretKommunale = ressursStateRepository.getAll().stream()
            .filter(RessursState::isKommunal)
            .filter(ressurs -> !nomMastered.isMastered(ressurs.navident()))
            .toList();

        var count = ikkeMastretKommunale.size();
        if (commit) {
            ikkeMastretKommunale.forEach(ressurs -> nomMastered.markRessursAsMasteredInNom(ressurs.navident()));
            var msg = "%s kommunale er nå markert som mastret".formatted(count);
            log.info(msg);
            return msg;
        } else {
            return "%s kommunale kan markeres som mastret".formatted(count);
        }
    }
}
