package no.nav.nom.apps.api.orgenhet.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.kostnadssted.KostnadsstedSynkroniseringService;
import no.nav.nom.apps.api.orgenhet.kostnadssted.KostnadsstedSynkroniseringService.KostnadsstedEndring;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin for synkronisering av kostnadssteder fra OrgKostMapping")
@RestController
@RequestMapping("/api/admin/kostnadssted")
@PreAuthorize("hasAuthority('ADMIN')")
public class TriggerKostnadsstedSynkronisering {

    private final KostnadsstedSynkroniseringService kostnadsstedSynkroniseringService;

    public TriggerKostnadsstedSynkronisering(KostnadsstedSynkroniseringService kostnadsstedSynkroniseringService) {
        this.kostnadsstedSynkroniseringService = kostnadsstedSynkroniseringService;
    }

    @Operation(
        description = "Henter kostnadssted-mappinger fra OrgKostMapping og videreformidler endringer mot NOM",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/synkroniser")
    public ResponseEntity<List<KostnadsstedEndring>> synkroniserKostnadssteder() {
        return ResponseEntity.ok(kostnadsstedSynkroniseringService.synkroniserKostnadssteder());
    }
}

