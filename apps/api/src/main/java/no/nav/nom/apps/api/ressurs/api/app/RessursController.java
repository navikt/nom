package no.nav.nom.apps.api.ressurs.api.app;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletResponse;

import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningTypeAndSektorCombinationException;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import no.nav.nom.apps.api.ressurs.api.dto.RessursLeder;
import no.nav.nom.apps.api.ressurs.api.exceptions.DuplicateRessursException;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.ressurs.api.exceptions.NomMasteredException;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.apps.api.topicreader.ReadTopicRequest;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
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

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import static no.nav.nom.apps.api.common.AppConfig.SECURE_LOGS;

@Tag(name = "Ressurs")
@RestController
@RequestMapping(value={"/app/ressurser"})
public class RessursController {

    private final static Logger log = LoggerFactory.getLogger(RessursController.class);
    private final RessursRestService ressursRestService;
    private final RessursExcelGenerator ressursExcelGenerator;
    private final AuthResolver authResolver;

    public RessursController(
        RessursRestService ressursRestService, RessursExcelGenerator ressursExcelGenerator, AuthResolver authResolver
    ) {
        this.ressursRestService = ressursRestService;
        this.ressursExcelGenerator = ressursExcelGenerator;
        this.authResolver = authResolver;
    }

    @GetMapping
    @Operation(
        description = "Henter ressurser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ressurs"),
    })
    public ResponseEntity<List<RessursState>> getRessurser(RessursFilter ressursFilter) {

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
        @ApiResponse(responseCode = "403", description = "Feil scope")
    })
    /*@PreAuthorize(
        "hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')"
    )*/
    public ResponseEntity<OpprettRessursResponse> opprettRessurs(
        @RequestBody OpprettRessursRequest request) {
        if (!authResolver.kanOppretteRessurs(request.orgEnhetId())) {
            throw new AccessDeniedException("Ikke tilgang til å opprette ressurs for angitt orgenhet");
        }
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
        "hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')"
    )
    public ResponseEntity<StandardResponse> avvikleRessurs(
        @PathVariable String fid,
        @RequestBody AvvikleRessursRequest request) {
        if (!authResolver.kanRedigereRessurs(ressursRestService.findRessursByFid(fid).state().navident())) {
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
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
        "hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')"
    )
    public ResponseEntity<StandardResponse> updateVisningsnavn(
        @PathVariable String navident,
        @RequestBody UpdateVisningsnavnRequest updateVisningsnavnRequest
    ) {
        if (!authResolver.kanRedigereRessurs(navident)) {
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }

        ressursRestService.updateRessursNavn(navident, updateVisningsnavnRequest);

        return ResponseEntity.ok(StandardResponse.builder().message("Ok").build());
    }

    @PostMapping("{navident}/telefonnummer")
    @Operation(
        description = "Endrer primaryTelefonnummer for en ressurs",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
    })
    @PreAuthorize(
        "hasAuthority('NOM_UI_APP')"
    )
    public ResponseEntity<StandardResponse> updatePrimarytelefonnummer(
        @PathVariable String navident,
        @RequestBody UpdatePrimarytelefonnummer updatePrimarytelefonnummer
    ) {
        if (!authResolver.kanRedigereRessurs(navident) && !authResolver.kanSelvbetjene(navident)) {
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }

        ressursRestService.updateRessursPrimaryTelefonnummer(navident, updatePrimarytelefonnummer);

        return ResponseEntity.ok(StandardResponse.builder().message("Ok").build());
    }

    @GetMapping("/{navident}/telefonnummer")
    @Operation(
        description = "Henter en liste over alle telefonnummere til ressursen",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Telefonnummer"),
    })
    public ResponseEntity<List<Ressurs.Telefon>> getTelefonnummer(
        @PathVariable String navident
    ) {
        return ResponseEntity.ok(ressursRestService.getTelefonnummer(navident));
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
        @PathVariable String navident
    ){
        var ressurs = ressursRestService.findRessursByNavident(navident);

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
        @PathVariable String fid) {
        var ressurs = ressursRestService.findRessursByFid(fid);

        return ResponseEntity.ok(ressurs);
    }

    @PostMapping("/byListNavident")
    @Operation(
        description = "Henter ressurser med liste av navident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of Ressurs if found or empty list"),
    })
    public List<Ressurs> getRessursByListNavident(@Valid @RequestBody List<String> navidentList){
        return ressursRestService.findRessurserByNavident(navidentList);
    }

    @PostMapping("/byListNavident/export")
    @Operation(
        description = "Genererer regneark med oversikt over ressurser for liste av navidenter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Exported file"),
    })
    public void getRessursByListNavidentExport(HttpServletResponse response, @Valid @RequestBody ExcelExportRequest excelExportRequest) throws IOException {
        try {
            var navidenter = excelExportRequest.navidenter();
            var ressurser = ressursRestService.findRessurserByNavident(navidenter);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=skjerminger.xlsx");
            ressursExcelGenerator.generate(response, ressurser, navidenter, excelExportRequest.topOrgEnhetId());
        } catch (Exception e) {
            log.error(SECURE_LOGS, "Feil ved excel export", e);
            log.error("Failed to generate export file");
            response.sendError(500);
        }
    }

    @GetMapping("/read-topic")
    @Operation(
        description = "Henter records fra ressurs event-source",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('ADMIN')")
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
        "hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP')"
    )
    public ResponseEntity<StandardResponse> updateFysiskeBrukersteder(
        @PathVariable String fid,
        @RequestBody UpdateFysiskeBrukerstederRequest updateFysiskeBrukerstederRequest
    ) {
        if (!authResolver.kanRedigereRessurs(ressursRestService.findRessursByFid(fid).state().navident())) {
            throw new AccessDeniedException("Ikke tilgang til å redigere ressurs");
        }

        ressursRestService.updateFysiskeBrukersteder(fid, updateFysiskeBrukerstederRequest);

        return ResponseEntity.ok(
            StandardResponse.builder()
                .message(("Successfully changed fysiske brukersteder"))
                .build()
        );
    }

    @GetMapping("/byFysiskbrukersted/{fysiskBrukerstedNr}")
    @Operation(
        description = "Hent alle ressurser som har angitt fysisk brukersted som primære fysisk brukersted",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "403"),
    })
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<RessursState>> getRessurserByFysiskbrukersted(
        @PathVariable String fysiskBrukerstedNr
    ) {
        return ResponseEntity.ok(ressursRestService.getRessurserMedFysiskBrukersted(fysiskBrukerstedNr));
    }

    @GetMapping("/byEnhetAndFysiskbrukersted/{orgenhetId}/{fysiskBrukerstedNr}")
    @Operation(
        description = "Hent alle ressurser som har aktiv DOl på angitt enhet og angitt fysisk brukersted som primære fysisk brukersted",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "403"),
    })
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<RessursState>> getRessurserByEnhetAndFysiskbrukersted(
        @PathVariable String orgenhetId,
        @PathVariable String fysiskBrukerstedNr
    ) {
        return ResponseEntity.ok(ressursRestService.getRessurserPaaEnhetMedFysiskBrukersted(orgenhetId, fysiskBrukerstedNr));
    }

    @PostMapping("/updateFysiskBrukerstedForRessurserByFysiskbrukersted/{fraFysiskBrukerstedNr}")
    @Operation(
        description = "Flytt alle ressursers primære fysiske brukersted som har angitt fysisk brukersted som primære fysisk brukersted",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "403"),
    })
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse> updateFysiskBrukerstedForRessurserByFysiskbrukersted(
        @PathVariable String fraFysiskBrukerstedNr,
        @RequestBody UpdatePrimaryFysiskBrukerstedRequest updatePrimaryFysiskBrukerstedRequest
    ) {
        ressursRestService.updateFysiskBrukerstedForRessurserByFysiskbrukersted(
            fraFysiskBrukerstedNr,
            updatePrimaryFysiskBrukerstedRequest.newPrimaryFysiskBrukersted()
        );

        return ResponseEntity.ok(
            StandardResponse.builder()
                .message(("Successfully changed fysisk brukersted for ressurser"))
                .build()
        );
    }

    @PostMapping("/updateFysiskBrukerstedForRessurserByEnhetAndFysiskbrukersted/{orgenhetId}/{fraFysiskBrukerstedNr}")
    @Operation(
        description = "Flytt alle ressursers primære fysiske brukersted som har aktiv DOl på angitt enhet og angitt fysisk brukersted som primære fysisk brukersted",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "403"),
    })
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse> updateFysiskBrukerstedForRessurserByEnhetAndFysiskbrukersted(
        @PathVariable String orgenhetId,
        @PathVariable String fraFysiskBrukerstedNr,
        @RequestBody UpdatePrimaryFysiskBrukerstedRequest updatePrimaryFysiskBrukerstedRequest
    ) {
        ressursRestService.updateFysiskBrukerstedForRessurserByEnhetAndFysiskbrukersted(
            orgenhetId,
            fraFysiskBrukerstedNr,
            updatePrimaryFysiskBrukerstedRequest.newPrimaryFysiskBrukersted()
        );

        return ResponseEntity.ok(
            StandardResponse.builder()
                .message(("Successfully changed fysisk brukersted for ressurser"))
                .build()
        );
    }

    @PostMapping("/updateFysiskBrukerstedForRessurserByNavidentList")
    @Operation(
        description = "Flytt angitte ressursers primære fysiske brukersted",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "403"),
    })
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandardResponse> updateFysiskBrukerstedForRessurserByNavidentList(
        @RequestBody UpdatePrimaryFysiskBrukerstedForRessurserRequest request
    ) {
        ressursRestService.updateFysiskBrukerstedForRessurserByNavidentList(
            request.navidenter(),
            request.newPrimaryFysiskBrukersted()
        );

        return ResponseEntity.ok(
            StandardResponse.builder()
                .message(("Successfully changed fysisk brukersted for ressurser"))
                .build()
        );
    }

    @GetMapping("/{navident}/remedyEnhet")
    @Operation(
        description = "Henter en ressursens remedyEnhet basert på DOL",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "remedyEnhet"),
    })
    public ResponseEntity<String> getRemedyEnhet(
        @PathVariable String navident
    ) {
        if (!ressursRestService.ressursExists(navident)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ressursRestService.getRemedyEnhet(navident));
    }

    @GetMapping("/{navident}/epost")
    @Operation(
        description = "Henter en ressursens epost",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "epost"),
    })
    public ResponseEntity<String> getEpost(
        @PathVariable String navident
    ) {
        if (!ressursRestService.ressursExists(navident)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ressursRestService.getEpost(navident));
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
        log.error("Intern feil ved opprettelse av ny ressurs", exception);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder().message("Server error").build());
    }
}
