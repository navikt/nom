package no.nav.nom.apps.api.orgtilknytning.api.app;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOppdaterOrgtilknytningerForRessursRequestException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningTypeAndSektorCombinationException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningStartDateWhenNewRemedyEnhetException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.OrgtilknytningNotFoundException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.OrgtilknytningPeriodeOverlapException;
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
    private final AuthResolver authResolver;

    public OrgtilknytningController(Unleash unleash,
                                    OrgtilknytningRestService orgtilknytningRestService,
                                    AuthResolver authResolver
    ) {
        this.unleash = unleash;
        this.orgtilknytningRestService = orgtilknytningRestService;
        this.authResolver = authResolver;
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

    @PostMapping
    @Operation(
        description = "Oppretter orgtilknytning",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
    })
    @PreAuthorize(
        "hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')"
    )
    public ResponseEntity<StandardResponse> addOrgtilknytning(
        @RequestBody LeggTilOrgtilknytningRequestOld leggTilOrgtilknytningRequest
    ) {
        if (!authResolver.kanRedigereRessurs(leggTilOrgtilknytningRequest.navident())) {
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }

        if (unleash.isEnabled(UNLEASH_EDIT_ORGTILKNYTNING_FOR_EXTERNAL)) {
            orgtilknytningRestService.addOrgtilknyting(leggTilOrgtilknytningRequest);

            return ResponseEntity.ok(StandardResponse.builder().message("Ok").build());
        }

        throw new AccessDeniedException("Opprettelse av orgtilknytning er blokkert");
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
        "hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')"
    )
    public ResponseEntity<StandardResponse> updateDato(
        @PathVariable String fid,
        @RequestBody EndreOrgtilknytningDatoRequest endreOrgtilknytningDatoRequest
    ) {
        if (!authResolver.kanRedigereRessurs(orgtilknytningRestService.getOrgtilknytning(fid).navident())) {
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }

        if (unleash.isEnabled(UNLEASH_EDIT_ORGTILKNYTNING_FOR_EXTERNAL)) {
            orgtilknytningRestService.updateOrgtilknytingDato(fid, endreOrgtilknytningDatoRequest);

            return ResponseEntity.ok(StandardResponse.builder().message("Ok").build());
        }

        throw new AccessDeniedException("Endring av orgtilknytning er blokkert");
    }

    @PostMapping("/{fid}/tilknytningtype")
    @Operation(
        description = "Endrer orgtilknytningType for en orgtilknytning",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
    })
    @PreAuthorize(
        "hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')"
    )
    public ResponseEntity<StandardResponse> updateOrgtilknytningType(
        @PathVariable String fid,
        @RequestBody EndreTilknytningtypeRequest endreTilknytningtypeRequest
    ) {
        if (!authResolver.kanRedigereRessurs(orgtilknytningRestService.getOrgtilknytning(fid).navident())) {
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }

        if (unleash.isEnabled(UNLEASH_EDIT_ORGTILKNYTNING_FOR_EXTERNAL)) {
            orgtilknytningRestService.updateOrgtilknytningType(fid, endreTilknytningtypeRequest);

            return ResponseEntity.ok(StandardResponse.builder().message("Ok").build());
        }

        throw new AccessDeniedException("Endring av orgtilknytning er blokkert");
    }

    @GetMapping("/{fid}")
    @Operation(
        description = "Direkte oppslag på orgtilknytning med fid",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
    })
    public ResponseEntity<OrgtilknytningState> getOrgtilknytning(
        @PathVariable String fid
    ) {
        return ResponseEntity.ok(
            orgtilknytningRestService.getOrgtilknytning(fid)
        );
    }

    @PostMapping("/forRessurs/{navident}")
    @Operation(
        description = "Sender inn komplett øyeblikksbilde av hvordan alle orgtilknytningene til en ressurs skal se ut",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
    })
    @PreAuthorize(
        "hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')"
    )
    public ResponseEntity<StandardResponse> updateOrgtilknytninger(
        @PathVariable String navident,
        @Valid @RequestBody OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest
    ) {
        if (unleash.isEnabled("nom.ressurs.ny-move-prosess")) {
            var isLeder = authResolver.kanRedigereRessurs(navident);

            if (unleash.isEnabled(UNLEASH_EDIT_ORGTILKNYTNING_FOR_EXTERNAL)) {
                orgtilknytningRestService.oppdaterOrgtilknytningerForRessurs(navident, oppdaterOrgtilknytningerForRessursRequest, isLeder);
                return ResponseEntity.ok(StandardResponse.builder().message("Orgtilknytninger for ressurs oppdatert").build());
            }
        } else {
            if (!authResolver.kanRedigereRessurs(navident)) {
                throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
            }

            if (unleash.isEnabled(UNLEASH_EDIT_ORGTILKNYTNING_FOR_EXTERNAL)) {
                orgtilknytningRestService.oppdaterOrgtilknytningerForRessurs(navident, oppdaterOrgtilknytningerForRessursRequest);
                return ResponseEntity.ok(StandardResponse.builder().message("Orgtilknytninger for ressurs oppdatert").build());
            }
        }
        throw new AccessDeniedException("Endring av orgtilknytning er blokkert");
    }


    // TODO: Lage global ControllerAdvice
    @ExceptionHandler({
        IllegalArgumentException.class,
        NoSuchElementException.class,
        InvalidSektorException.class,
        InvalidOrgtilknytningTypeAndSektorCombinationException.class,
        InvalidOrgtilknytningStartDateWhenNewRemedyEnhetException.class,
        InvalidOppdaterOrgtilknytningerForRessursRequestException.class
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

    @ExceptionHandler({
        OrgtilknytningPeriodeOverlapException.class,
    })
    public ResponseEntity<StandardResponse> handleConflict(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
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
