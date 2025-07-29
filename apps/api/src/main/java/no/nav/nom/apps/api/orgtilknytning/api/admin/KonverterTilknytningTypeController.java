package no.nav.nom.apps.api.orgtilknytning.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgtilknytning.job.KonverterEksternSamarbeidspartnerTilknytningtype;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/admin")
@Slf4j
public class KonverterTilknytningTypeController {
    private final KonverterEksternSamarbeidspartnerTilknytningtype konverterEksternSamarbeidspartnerTilknytningtype;

    public KonverterTilknytningTypeController(
        KonverterEksternSamarbeidspartnerTilknytningtype konverterEksternSamarbeidspartnerTilknytningtype
    ) {
        this.konverterEksternSamarbeidspartnerTilknytningtype = konverterEksternSamarbeidspartnerTilknytningtype;
    }

    @Operation(
        description = "Konverter Tilknytningstyper for eksterne samarbeidspartnere til Helfo, skatt og Riksrevisjonen",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PostMapping("/konverter-tilknytning-typer")
    public ResponseEntity<StandardResponse> konverterTilknytningTyper() {
        var result = konverterEksternSamarbeidspartnerTilknytningtype.konverterTilknytningtyperTilEksterneOrgEnheter();
        return ResponseEntity.ok(StandardResponse.builder().message("Konvertert tilknytningstyper vellykket\n" + result).build());
    }

    @ExceptionHandler(Exception.class)
    private ResponseEntity<StandardResponse> handleServerError(Exception exception) {
        log.error("Intern feil ved konvertering av Tilknytningtyper" , exception);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder().message("Server error - KonverterTilknytningTypeController").build());
    }
}
