package no.nav.nom.apps.sfintegrasjon.admin.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.sfintegrasjon.organisasjon.diff.OrgenhetComparer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/diff")
@Tag(name = "Diff test endepunkter", description = "Endpoints for å teste diff mellom NOM og SF.")
public class DiffController {
    private final OrgenhetComparer orgenhetComparer;

    public DiffController(OrgenhetComparer orgenhetComparer) {
        this.orgenhetComparer = orgenhetComparer;
    }

    @GetMapping(value = "/orgenhet/{nomOrgEnhetId}", produces = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Diff NOM og SF for angitt enhet"
    )
    public ResponseEntity<String> diffOrgenhet(@PathVariable String nomOrgEnhetId) {
        var diffResult = orgenhetComparer.compare(nomOrgEnhetId);
        return ResponseEntity.ok(orgenhetComparer.compare(nomOrgEnhetId).report());
    }
}
