package no.nav.nom.apps.api.orgtilknytning.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.api.app.MasseFlyttingRequest;
import no.nav.nom.apps.api.orgtilknytning.api.app.OrgtilknytningRestService;
import no.nav.nom.apps.api.orgtilknytning.job.OTUtenforHierarki;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/admin")
@Slf4j
public class OrgtilknytningerUtenforHierarkiController {
    private final OTUtenforHierarki otUtenforHierarki;
    private final RessursStateRepository ressursStateRepository;
    private final OrgenhetService orgenhetService;
    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final OrgtilknytningRestService orgtilknytningRestService;

    public OrgtilknytningerUtenforHierarkiController(
        OTUtenforHierarki otUtenforHierarki,
        RessursStateRepository ressursStateRepository,
        OrgenhetService orgenhetService, RemedyEnhetMapperService remedyEnhetMapperService, OrgtilknytningRestService orgtilknytningRestService
    ) {
        this.otUtenforHierarki = otUtenforHierarki;
        this.ressursStateRepository = ressursStateRepository;
        this.orgenhetService = orgenhetService;
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.orgtilknytningRestService = orgtilknytningRestService;
    }

    @Operation(
        description = "List orgtilknytninger som ligger utenfor aktiv organisasjonshierarki",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/OrgtilknytningerUtenforHierarki")
    public ResponseEntity<List<OrgtilknytningerUtenforHierarkiResponse>> OrgtilknytningerUtenforHierarki() {
        otUtenforHierarki.findOTUtenforHierarki(); // Alltid refresh
        var response = getHierarkiResponse();

        return ResponseEntity.ok(response);
    }

    private List<OrgtilknytningerUtenforHierarkiResponse> getHierarkiResponse() {
        return otUtenforHierarki.getOtUtenforHierarkiMedAlternativOrgenhet().entrySet().stream()
            .map((entry) -> {
                var ot = entry.getKey();
                var otEnhet = Objects.requireNonNull(orgenhetService.getById(ot.orgEnhetId()).orElse(null));
                var ressurs = Objects.requireNonNull(ressursStateRepository.findByNavident(ot.navident()).orElse(null));
                var otRemedyEnhet = remedyEnhetMapperService.findByNomId(otEnhet.getId());
                var alternativOrgenhet = entry.getValue();
                var alternativRemedyEnhet = alternativOrgenhet == null ? java.util.Optional.<RemedyEnhet>empty() : remedyEnhetMapperService.findByNomId(
                    alternativOrgenhet.getId());
                return OrgtilknytningerUtenforHierarkiResponse.from(
                    ressurs,
                    ot,
                    otEnhet,
                    otRemedyEnhet.map(RemedyEnhet::remedyEnhetId).orElse(null),
                    alternativOrgenhet,
                    alternativRemedyEnhet.map(RemedyEnhet::remedyEnhetId).orElse(null)
                );
            })
            .toList();
    }
}
