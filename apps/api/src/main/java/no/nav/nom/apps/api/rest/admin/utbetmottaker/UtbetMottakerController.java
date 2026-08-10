package no.nav.nom.apps.api.rest.admin.utbetmottaker;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: Utbetalingsmottakere fra bucket")
@Slf4j
@RestController
@RequestMapping("/api/admin/utbetmottaker")
@PreAuthorize("hasAuthority('ADMIN')")
public class UtbetMottakerController {

    private final UtbetMottakerBucketService utbetMottakerBucketService;

    public UtbetMottakerController(UtbetMottakerBucketService utbetMottakerBucketService) {
        this.utbetMottakerBucketService = utbetMottakerBucketService;
    }

    @Operation(
            description = "Les inn utbetalingsmottakere fra oppgitt fil i GCS-bucket. "
                    + "Rader med samme fødselsnummer leses inn én gang og markeres som duplikat. "
                    + "Returnerer oppsummering med antall totalt, unike og duplikater.",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/lesInnFraFil")
    public UtbetMottakerInnlesningResultat lesInnFraFil(
            @RequestParam String filnavn
    ) {
        log.info("Manuell innlesning av utbetalingsmottakere, filnavn={}", filnavn);
        return utbetMottakerBucketService.lesFraFil(filnavn);
    }

    @ExceptionHandler(UtbetMottakerBucketException.class)
    public ResponseEntity<String> handleBucketFeil(UtbetMottakerBucketException e) {
        log.error("Feil ved innlesning av utbetalingsmottakere: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}
