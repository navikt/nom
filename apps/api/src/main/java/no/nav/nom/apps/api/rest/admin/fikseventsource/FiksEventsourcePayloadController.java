package no.nav.nom.apps.api.rest.admin.fikseventsource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin for korrigeringer")
@RestController
@RequestMapping("/api/admin/fiksEventsourcePayload")
@PreAuthorize("hasAuthority('ADMIN')")
public class FiksEventsourcePayloadController {

    private final FiksEventsourcePayloadService fiksEventsourcePayloadService;

    public FiksEventsourcePayloadController(FiksEventsourcePayloadService fiksEventsourcePayloadService) {
        this.fiksEventsourcePayloadService = fiksEventsourcePayloadService;
    }

    @Operation(
        description = "Reparerer feilaktige eventsource recovery-records i database. "
            + "Leser korrekt data fra Kafka topic og oppdaterer DB-records som har feil payload (Jackson type metadata i stedet for faktisk JSON). "
            + "Bruk 'table' for å velge hvilken recovery-tabell som skal repareres. "
            + "Skriv CONFIRM for å utføre oppdatering, alt annet gir dry run.",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping
    public String fiksEventsourcePayload(
        @Parameter(description = "Velg eventsource-tabell: RESSURS, ORGENHET, ORGENHET_LEDER, ORGTILKNYTNING, ORGANISERING, AUTHORIZE")
        @RequestParam EventSourceTable table,
        @Parameter(description = "Skriv CONFIRM for å utføre oppdatering, alt annet gir dry run")
        @RequestParam String confirm
    ) {
        boolean dryRun = !"CONFIRM".equals(confirm);
        return fiksEventsourcePayloadService.repair(table, dryRun);
    }
}

