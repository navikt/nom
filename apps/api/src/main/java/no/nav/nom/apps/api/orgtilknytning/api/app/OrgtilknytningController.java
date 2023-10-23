package no.nav.nom.apps.api.orgtilknytning.api.app;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.OrgtilknytningNotFoundException;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Tag(name = "Orgtilknytning")
@RestController
@RequestMapping(value={"/app/orgtilknytninger"})
public class OrgtilknytningController {

    private final static Logger logger = LoggerFactory.getLogger(OrgtilknytningController.class);
    private static final String UNLEASH_EDIT_ORGTILKNYTNING_FOR_EXTERNAL = "nom.ressurs.edit-external";

    private final Unleash unleash;
    private final OrgtilknytningRestService orgtilknytningRestService;

    public OrgtilknytningController(Unleash unleash, OrgtilknytningRestService orgtilknytningRestService) {
        this.unleash = unleash;
        this.orgtilknytningRestService = orgtilknytningRestService;
    }

    @GetMapping()
    @Operation(
        description = "Henter orgtilknytninger",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
    })
    public ResponseEntity<List<OrgtilknytningState>> getOrgtilknytninger(
        @RequestParam Optional<String> navident
    ) {
        return ResponseEntity.ok(
            orgtilknytningRestService.getOrgtilknytninger(navident)
        );
    }

    @PostMapping("/{fid}/dato")
    @Operation(
        description = "Endrer dato for en orgtilknytning",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
    })
    @PreAuthorize(
        "hasAuthority('MUTATE_RESSURS')"
    )
    public ResponseEntity<StandardResponse> updateDato(
        @PathVariable String fid,
        @RequestBody EndreOrgtilknytningDatoRequest endreOrgtilknytningDatoRequest
    ) {
        if (unleash.isEnabled(UNLEASH_EDIT_ORGTILKNYTNING_FOR_EXTERNAL)) {
            orgtilknytningRestService.updateOrgtilknyting(fid, endreOrgtilknytningDatoRequest);

            return ResponseEntity.ok(StandardResponse.builder().message("Ok").build());
        }

        throw new AccessDeniedException("Endring av orgtilknytning er blokkert");
    }


    // TODO: Lage global ControllerAdvice
    @ExceptionHandler({
        IllegalArgumentException.class,
        NoSuchElementException.class,
        InvalidSektorException.class
    })
    public ResponseEntity<StandardResponse> handleBadRequest(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(StandardResponse.builder().message(exception.getMessage()).build());
    }

    @ExceptionHandler({
        RessursNotFoundException.class,
        OrgtilknytningNotFoundException.class
    })
    public ResponseEntity<StandardResponse> handleNotFound(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(StandardResponse.builder().message(exception.getMessage()).build());
    }

    @ExceptionHandler({
        AccessDeniedException.class,
    })
    public ResponseEntity<StandardResponse> handleForbidden(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(StandardResponse.builder().message(exception.getMessage()).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardResponse> handleServerError(Exception exception) {
        logger.error("Uforutsett feil", exception);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder().message("Server error").build());
    }

}
