package no.nav.nom.apps.api.ressurs.api.app;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.contract.nom.state.RessursState;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@Tag(name = "Ressurs")
@RestController
@RequestMapping(value={"/app/ressurs"})
public class RessursController {

    private final static Logger log = LoggerFactory.getLogger(RessursController.class);
    public static final String UNLEASH_EDIT_RESSURS = "nom.ressurs.edit-external";
    private final RessursRestService ressursRestService;
    private final Unleash unleash;

    public RessursController(
        RessursRestService ressursRestService,
        Unleash unleash
    ) {
        this.ressursRestService = ressursRestService;
        this.unleash = unleash;
    }

    @PostMapping
    @Operation(
        description = "Oppretter ressurs eller returnerer det som allerede eksisterer",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Returns existing"),
        @ApiResponse(responseCode = "201", description = "Created"),
    })
    @PreAuthorize(
        "hasAuthority('MUTATE_RESSURS')"
    )
    public ResponseEntity<OpprettRessursResponse> opprettRessurs(
        @RequestBody OpprettRessursRequest request) {
        return ResponseEntity.ok(toResponse(ressursRestService.createRessursAndNotify(request)));
    }

    @PostMapping("/{navident}/navn")
    @Operation(
        description = "Endrer navn for en ressurs",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
    })
    @PreAuthorize(
        "hasAuthority('MUTATE_RESSURS')"
    )
    public ResponseEntity<StandardResponse> updateVisningsnavn(
        @PathVariable String navident,
        @RequestBody UpdateVisningsnavnRequest updateVisningsnavnRequest
    ) {
        if (!unleash.isEnabled(UNLEASH_EDIT_RESSURS)) {
            throw new AccessDeniedException("Endring av ressurs er blokkert");
        }

        ressursRestService.updateRessursNavn(navident, updateVisningsnavnRequest);

        return ResponseEntity.ok(StandardResponse.builder().message("Ok").build());
    }

    @GetMapping("/{navident}")
    @Operation(
        description = "Henter ressurs med navident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ressurs"),
    })
    public ResponseEntity<RessursState> getRessurs(@PathVariable String navident){
        return ResponseEntity.ok(ressursRestService.findRessursByNavident(navident));
    }

    private OpprettRessursResponse toResponse(RessursState ressurs) {
        if (ressurs == null) {
            return null;
        }

        return OpprettRessursResponse.builder()
            .personident(ressurs.personident())
            .navident(ressurs.navident())
            .visningsfornavn(
                ressurs.visningsnavn().mellomnavn().map(m -> ressurs.visningsnavn().fornavn() + " " + m).orElse(ressurs.visningsnavn().fornavn())
            )
            .visningsetternavn(ressurs.visningsnavn().etternavn())
            .sektor(ressurs.sektor())
            .build();
    }

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


    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardResponse> handleServerError(Exception exception) {
        log.error("Intern feil ved opprettelse av ny ressurs", exception);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder().message("Server error").build());
    }
}
