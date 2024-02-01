package no.nav.nom.apps.api.ressurs.api.app;

import com.fasterxml.jackson.databind.JsonNode;
import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningTypeAndSektorCombinationException;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import no.nav.nom.apps.api.ressurs.api.dto.RessursLeder;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.apps.api.topicreader.ReadTopicRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@Tag(name = "Ressurs")
@RestController
@RequestMapping(value={"/app/ressurser"})
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

    @GetMapping
    @Operation(
        description = "Henter ressurser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ressurs"),
    })
    public ResponseEntity<List<Ressurs>> getRessurser(RessursFilter ressursFilter) {

        return ResponseEntity.ok(ressursRestService.findRessurser(ressursFilter));
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

    @PostMapping("/{fid}/avvikle")
    @Operation(
        description = "Avvikler en ressurs ved å sette sluttdato",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
    })
    @PreAuthorize(
        "hasAuthority('MUTATE_RESSURS')"
    )
    public ResponseEntity<StandardResponse> avvikleRessurs(
        @PathVariable String fid,
        @RequestBody AvvikleRessursRequest request) {
        if (!unleash.isEnabled(UNLEASH_EDIT_RESSURS)) {
            throw new AccessDeniedException("Endring av ressurs er blokkert");
        }
        ressursRestService.avvikleRessurs(fid, request);
        return ResponseEntity.ok(StandardResponse.builder().message("Ok").build());
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

    @GetMapping("/byNavident/{navident}")
    @Operation(
        description = "Henter ressurs med navident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ressurs"),
    })
    public ResponseEntity<Ressurs> getRessursByNavident(
        @PathVariable String navident, @RequestParam(defaultValue = "true") Boolean obfuscatePersonident
    ){
        var ressurs = ressursRestService.findRessursByNavident(navident, obfuscatePersonident);

        return ResponseEntity.ok(ressurs);
    }

    @GetMapping("/byFid/{fid}")
    @Operation(
        description = "Henter ressurs med fid",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ressurs"),
    })
    public ResponseEntity<Ressurs> getRessursByFid(
        @PathVariable String fid, @RequestParam(defaultValue = "true") Boolean obfuscatePersonident) {
        var ressurs = ressursRestService.findRessursByFid(fid, obfuscatePersonident);

        return ResponseEntity.ok(ressurs);
    }

    @GetMapping("/read-topic")
    @Operation(
        description = "Henter records fra ressurs event-source",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
     public ResponseEntity<List<JsonNode>> readRessursTopic(@Valid ReadTopicRequest request){
        return ResponseEntity.ok(ressursRestService.getRessursEvents(request));
    }

    @GetMapping("/lederFor/{navident}")
    @Operation(
        description = "Henter ledere for en ressurs med navident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of Ressurs if found or empty list"),
    })
    public List<RessursLeder> getRessursLedere(@PathVariable String navident){
        return ressursRestService.getLedereForRessurs(navident);
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

    @PostMapping("/{fid}/fysiskebrukersteder")
    @Operation(
        description = "Overskriver fysiske brukersteder for en ressurs med navident.",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)

    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
    })
    @PreAuthorize(
        "hasAuthority('MUTATE_RESSURS')"
    )
    public ResponseEntity<StandardResponse> updateFysiskeBrukersteder(
        @PathVariable String fid,
        @RequestBody UpdateFysiskeBrukerstederRequest updateFysiskeBrukerstederRequest
    ) {
        if (!unleash.isEnabled(UNLEASH_EDIT_RESSURS)) {
            throw new AccessDeniedException("Endring av ressurs er blokkert");
        }

        ressursRestService.updateFysiskeBrukersteder(fid, updateFysiskeBrukerstederRequest);

        return ResponseEntity.ok(
            StandardResponse.builder()
                .message(("Successfully changed fysiske brukersteder"))
                .build()
        );
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        NoSuchElementException.class,
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


    @ExceptionHandler(Exception.class)
    private ResponseEntity<StandardResponse> handleServerError(Exception exception) {
        log.error("Intern feil ved opprettelse av ny ressurs", exception);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder().message("Server error").build());
    }
}
