package no.nav.nom.apps.api.orgenhetleder.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.recoverybackup.OrgEnhetLederEventSourceRecoveryService;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin for recovery av event source fra en database backup")
@Slf4j
@RestController
@RequestMapping("/api/admin/recovery/")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class OrgEnhetLederRecoveryController {

    private final OrgEnhetLederEventSourceRecoveryService orgEnhetLederEventSourceRecoveryService;

    @Operation(
        description = "Start recovery av OrgEnhetLeder Event Source fra back up",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("orgEnhetLeder")
    public ResponseEntity<StandardResponse> recoveryEventSource(@RequestParam String confirm, @RequestParam boolean dryRun) {
        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(StandardResponse.builder().message("Ikke skrevet CONFIRM riktig").build());
        }
        orgEnhetLederEventSourceRecoveryService.replayAllEventsource(dryRun);
        return ResponseEntity.ok(StandardResponse.builder().message("Recovery vellykket").build());
    }

    @ExceptionHandler(Exception.class)
    private ResponseEntity<StandardResponse> handleServerError(Exception e) {
        log.error("Feil skjedde under recovery av orgEnhetLederEvtsrc med følgende feilmelding {} \n ruller tilbake", e.getMessage());

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder()
                .message("Server error: %s".formatted(e.getMessage()))
                .build());
    }
}
