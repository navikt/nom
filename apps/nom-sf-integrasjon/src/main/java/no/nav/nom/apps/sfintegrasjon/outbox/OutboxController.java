package no.nav.nom.apps.sfintegrasjon.outbox;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.sfintegrasjon.outbox.domain.SfStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "Outbox API", description = "Endpoints for Team ORG Admins to check and resend data.")
@PreAuthorize("hasAuthority('ADMIN')")
@RestController
@RequestMapping("/api/admin/outbox")
public class OutboxController {

    private final OutboxRestService outboxRestService;

    public OutboxController(OutboxRestService outboxRestService) {
        this.outboxRestService = outboxRestService;
    }

    @GetMapping
    public ResponseEntity<List<SfOutboxResponse>> hentAlleSfOutbox() {
        return ResponseEntity.ok(outboxRestService.getAllSfOutBoxRows());
    }

    @GetMapping("/{orgEnhetId}")
    public ResponseEntity<List<SfOutboxResponse>> hentSfOutboxByOrgEnghetId(@PathVariable String orgEnhetId) {
        return ResponseEntity.ok(outboxRestService.getAllByOrgEnhetId(orgEnhetId));
    }

    @GetMapping("/getByStatus/{sfStatus}")
    public ResponseEntity<List<SfOutboxResponse>> getByStatus(@PathVariable SfStatus sfStatus) {
        return ResponseEntity.ok(outboxRestService.getAllBySfStatus(sfStatus));
    }

    @GetMapping("/getAllRejected")
    public ResponseEntity<List<SfOutboxResponse>> getAllRejected() {
        return ResponseEntity.ok(outboxRestService.getAllRejected());
    }

    @GetMapping("/getAllFailed")
    public ResponseEntity<List<SfOutboxResponse>> getAllFailed() {
        return ResponseEntity.ok(outboxRestService.getAllFailed());
    }
}
