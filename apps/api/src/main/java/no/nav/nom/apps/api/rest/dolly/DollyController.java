package no.nav.nom.apps.api.rest.dolly;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningTypeAndSektorCombinationException;
import no.nav.nom.apps.api.ressurs.api.exceptions.DuplicateRessursException;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.ressurs.api.exceptions.NomMasteredException;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

import static no.nav.nom.apps.api.common.AppConfig.TEAM_LOGS;

@Slf4j
@Tag(name = "Dolly")
@RestController
@RequestMapping("/api/v1/dolly")
@PreAuthorize("hasAuthority('DOLLY') or hasAuthority('ADMIN')")
public class DollyController {

    private final DollyService dollyService;

    public DollyController(DollyService dollyService) {
        this.dollyService = dollyService;
    }

    @PostMapping("/hentRessurs")
    @Operation(
        description = "Slå opp ressurs basert på personident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Ok",
                content = {
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RessursDTO.class)
                    )
                }
            ),
            @ApiResponse(responseCode = "404", description = "Ressurs ikke funnet",
                content = {
                    @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = StandardResponse.class)
                    )
                }
            ),
            @ApiResponse(responseCode = "400", description = "Ugyldig input",
                content = {
                    @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = StandardResponse.class)
                    )
                }
            )
        }
    )
    public ResponseEntity<RessursDTO> hentRessurs(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Personident for ressurs", required = true,
                    content = @Content(
                            mediaType = "application/text",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "01011012345")
                    )
            )
            @RequestBody String personident
    ) {
        log.info(TEAM_LOGS, "hentRessurs kalt fra Dolly:\n{}", personident);
        if (personidentIsIllegal(personident)) ResponseEntity.badRequest().build();

        return ResponseEntity.ok(dollyService.getRessurs(personident));
    }

    @PostMapping("/opprettRessurs")
    @Operation(
        description = "Opprett ressurs for personident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Opprettet request mottatt"),
            @ApiResponse(responseCode = "409", description = "Ressurs allerede opprettet",
                content = {
                    @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = StandardResponse.class)
                    )
                }
            ),
            @ApiResponse(responseCode = "400", description = "Ugyldig input",
                content = {
                    @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = StandardResponse.class)
                    )
                }
            )
        }
    )
    public void opprettRessurs(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Ressurs som skal opprettes", required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DollyRessursRequest.class),
                    examples = @ExampleObject(value = "{ \"personident\": \"01011012345\", \"fornavn\": \"Tøff\", \"etternavn\": \"Kylling\" }")
                )
            )
            @RequestBody DollyRessursRequest request) {
        log.info(TEAM_LOGS, "opprettRessurs kalt fra Dolly:\n{}", request);
        if (personidentIsIllegal(request.personident())) ResponseEntity.badRequest().build();

        dollyService.opprettRessurs(request);
    }

    @PostMapping("/avsluttRessurs")
    @Operation(
        description = "Avslutt ressurs for personident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Avsluttet"),
            @ApiResponse(responseCode = "404", description = "Ressurs ikke funnet",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardResponse.class)
                            )
                    }
            ),
            @ApiResponse(responseCode = "400", description = "Ugyldig input",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = StandardResponse.class)
                            )
                    }
            )
        }
    )
    public void avsluttRessurs(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Ressurs som skal avsluttes", required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DollyRessursRequest.class),
                    examples = @ExampleObject(value = "{ \"personident\": \"01011012345\", \"fornavn\": \"Tøff\", \"etternavn\": \"Kylling\" }")
                )
            )
            @RequestBody DollyRessursRequest request
    ) {
        log.info(TEAM_LOGS, "avsluttRessurs kalt fra Dolly:\n{}", request);
        if (personidentIsIllegal(request.personident())) ResponseEntity.badRequest().build();

        dollyService.avvikleRessurs(request);
    }

    private static boolean personidentIsIllegal(String personident) {
        if (personident == null || personident.isBlank()) {
            log.error("Personident er tom");
            return true;
        }
        return false;
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        NoSuchElementException.class,
        NomMasteredException.class,
        InvalidSektorException.class,
        InvalidOrgtilknytningTypeAndSektorCombinationException.class
    })
    public ResponseEntity<StandardResponse> handleBadRequest(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
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

    @ExceptionHandler(
        {RessursNotFoundException.class}
    )
    public ResponseEntity<StandardResponse> handleNotFound(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(StandardResponse.builder().message(exception.getMessage()).build());
    }

    @ExceptionHandler({
        DuplicateRessursException.class,
    })
    public ResponseEntity<StandardResponse> handleConflict(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(StandardResponse.builder().message(exception.getMessage()).build());
    }

    @ExceptionHandler(Exception.class)
    private ResponseEntity<StandardResponse> handleServerError(Exception exception) {
        log.error("Intern feil i Dolly controller", exception);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder().message("Server error").build());
    }
}
