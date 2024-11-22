package no.nav.nom.apps.api.orgtilknytning.api.admin;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgtilknytning.job.ImportMissingOrgtilknytninger;
import no.nav.nom.apps.api.orgtilknytning.job.OTUtenforHierarki;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/admin")
@Slf4j
public class OrgtilknytningerUtenforHierarkiController {

    private final OTUtenforHierarki otUtenforHierarki;

    public OrgtilknytningerUtenforHierarkiController(OTUtenforHierarki otUtenforHierarki) {
        this.otUtenforHierarki = otUtenforHierarki;
    }

    @GetMapping("/OrgtilknytningerUtenforHierarki")
    public ResponseEntity<HashMap<OrgtilknytningState, IOrgenhet>> OrgtilknytningerUtenforHierarki() {
        return ResponseEntity.ok(otUtenforHierarki.getOtUtenforHierarkiMedAlternativOrgenhet());
    }
}
