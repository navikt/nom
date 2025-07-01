package no.nav.nom.apps.api.orgtilknytning.api.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOppdaterOrgtilknytningerForRessursRequestException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningSektorException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningStartDateWhenNewRemedyEnhetException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningTilknytningtypeException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningTypeAndSektorCombinationException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.OrgtilknytningNotFoundException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.OrgtilknytningPeriodeOverlapException;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
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

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedRoller;

@Slf4j
@Tag(name = "Orgtilknytning")
@RestController
@RequestMapping(value = {"/app/orgtilknytninger"})
public class OrgtilknytningController {

    private final static Logger logger = LoggerFactory.getLogger(OrgtilknytningController.class);

    private final OrgtilknytningRestService orgtilknytningRestService;
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningService orgtilknytningService;

    public OrgtilknytningController(OrgtilknytningRestService orgtilknytningRestService, RessursStateRepository ressursStateRepository,
                                    OrgtilknytningService orgtilknytningService
    ) {
        this.orgtilknytningRestService = orgtilknytningRestService;
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningService = orgtilknytningService;
    }

    @GetMapping()
    @Operation(
        description = "Henter orgtilknytninger",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200"),
        }
    )
    public ResponseEntity<List<OrgtilknytningState>> getOrgtilknytninger(
        @RequestParam(required = false) String navident
    ) {
        return ResponseEntity.ok(orgtilknytningRestService.getOrgtilknytninger(navident));
    }

    @PostMapping()
    @Operation(
        description = "Henter orgtilknytninger til liste av Navidenter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200"),
        }
    )
    public ResponseEntity<Map<String, List<OrgtilknytningState>>> getOrgtilknytningerForNavidenter(
        @RequestBody List<String> navidenter
    ) {
        return ResponseEntity.ok(orgtilknytningRestService.getOrgtilknytninger(navidenter));
    }

    @PostMapping("/{fid}/tilknytningtype")
    @Operation(
        description = "Endrer orgtilknytningType for en orgtilknytning",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200"),
        }
    )
    @PreAuthorize(
        "hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')"
    )
    public ResponseEntity<StandardResponse> updateOrgtilknytningType(
        @PathVariable String fid,
        @RequestBody EndreTilknytningtypeRequest endreTilknytningtypeRequest
    ) {
        validateKanEndreOrgtilknytningByFid(fid);

        orgtilknytningRestService.updateOrgtilknytningType(fid, endreTilknytningtypeRequest);
        return ResponseEntity.ok(StandardResponse.builder().message("Ok").build());
    }

    @GetMapping("/{fid}")
    @Operation(
        description = "Direkte oppslag på orgtilknytning med fid",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200"),
        }
    )
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
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200"),
        }
    )
    @PreAuthorize(
        "hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')"
    )
    public ResponseEntity<StandardResponse> updateOrgtilknytninger(
        @PathVariable String navident,
        @Valid @RequestBody OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest
    ) {
        validateKanEndreOrgtilknytninger(navident, oppdaterOrgtilknytningerForRessursRequest);

        orgtilknytningRestService.oppdaterOrgtilknytningerForRessurs(navident, oppdaterOrgtilknytningerForRessursRequest);
        return ResponseEntity.ok(StandardResponse.builder().message("Orgtilknytninger for ressurs oppdatert").build());
    }

    @PostMapping("/masseflytting")
    @Operation(
        description = "Masseflytting av ressurs fra en enhet til en annen",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Flyttingen ble utført"),
        @ApiResponse(responseCode = "400", description = "Feil i input"),
    })
    @PreAuthorize(
        // Hvis/når denne åpnes for andre, legg til tester som sjekker at
        // vanlig leder/delegert ikke kan flytte robot/vikafossen/utlKonsulent
        "hasAuthority('ADMIN')"
    )
    public ResponseEntity<MasseFlyttingResponse> masseflytting(
        @Valid @RequestBody MasseFlyttingRequest masseFlyttingRequest
    ) {
        validateKanMasseflytte(masseFlyttingRequest);

        var response = orgtilknytningRestService.masseflyttingAvRessurser(masseFlyttingRequest);
        return ResponseEntity.ok(response);
    }

    private void validateKanEndreOrgtilknytningByFid(String fid) {
        OrgtilknytningState ot = orgtilknytningRestService.getOrgtilknytning(fid);
        RessursState ressurs = ressursStateRepository.findByNavident(ot.navident())
            .orElseThrow(() -> RessursNotFoundException.navident(ot.navident()));

        if (!orgtilknytningRestService.kanEndreOrgtilknytning(ressurs.identType(), ressurs.navident())) {
            log.warn("{} med roller {} har ikke tilgang til å redigere orgtilknytning {} for ressurs - {} - {}, ",
                getAuthenticatedNavIdent(), getAuthenticatedRoller(), ot.fid(), ot.navident(), ressurs.identType());
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }
    }

    private void validateKanEndreOrgtilknytninger(String navident, OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        RessursState ressurs = ressursStateRepository.findByNavident(navident)
            .orElseThrow(() -> RessursNotFoundException.navident(navident));

        if (!orgtilknytningRestService.kanEndreOrgtilknytninger(ressurs.identType(), navident, oppdaterOrgtilknytningerForRessursRequest)) {
            log.warn("{} med roller {} har ikke tilgang til å redigere orgtilknytninger for ressurs - {} - {}, ",
                getAuthenticatedNavIdent(), getAuthenticatedRoller(), navident, ressurs.identType());
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }
    }

    private void validateKanMasseflytte(MasseFlyttingRequest masseFlyttingRequest) {
        masseFlyttingRequest.orgtilknytningerFid().forEach(otFid -> {
            var ot = orgtilknytningService.findByFid(otFid).orElseThrow(() -> OrgtilknytningNotFoundException.fid(otFid));

            var ressurs = ressursStateRepository.findByNavident(ot.navident())
                .orElseThrow(() -> RessursNotFoundException.navident(ot.navident()));

            if (!orgtilknytningRestService.kanEndreOrgtilknytning(ressurs.identType(), ot.navident())) {
                log.warn("{} med roller {} har ikke tilgang til å redigere orgtilknytninger for ressurs - {} - {}, ",
                    getAuthenticatedNavIdent(), getAuthenticatedRoller(), ot.navident(), ressurs.identType());
                throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
            }
        });
    }

    // TODO: Lage global ControllerAdvice
    @ExceptionHandler(
        {
            IllegalArgumentException.class,
            NoSuchElementException.class,
            InvalidSektorException.class,
            InvalidOrgtilknytningSektorException.class,
            InvalidOrgtilknytningTilknytningtypeException.class,
            InvalidOrgtilknytningTypeAndSektorCombinationException.class,
            InvalidOrgtilknytningStartDateWhenNewRemedyEnhetException.class,
            InvalidOppdaterOrgtilknytningerForRessursRequestException.class
        }
    )
    public ResponseEntity<StandardResponse> handleBadRequest(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(StandardResponse.builder().message(exception.getMessage()).build());
    }

    @ExceptionHandler(
        {
            RessursNotFoundException.class,
            OrgtilknytningNotFoundException.class
        }
    )
    public ResponseEntity<StandardResponse> handleNotFound(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(StandardResponse.builder().message(exception.getMessage()).build());
    }

    @ExceptionHandler(
        {
            AccessDeniedException.class,
        }
    )
    public ResponseEntity<StandardResponse> handleForbidden(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(StandardResponse.builder().message(exception.getMessage()).build());
    }

    @ExceptionHandler(
        {
            OrgtilknytningPeriodeOverlapException.class,
        }
    )
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
