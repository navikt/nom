package no.nav.nom.apps.api.rest.backend.ressurs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.apps.api.ressurs.email.RessursOpprettetEmailException;
import no.nav.nom.apps.api.ressurs.email.RessursOpprettetEmailService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Tag(name = "Ressurs")
@RestController
@RequestMapping("/backend/ressurs")
public class RessursController {

    private final static Logger log = LoggerFactory.getLogger(RessursController.class);

    private final IRessursService ressursService;
    private final RessursCommadService ressursCommadService;
    private final NIKClient nikClient;
    private final PdlClient pdlClient;
    private final OrgenhetService orgenhetService;
    private final RessursOpprettetEmailService ressursOpprettetEmailService;

    public RessursController(
        IRessursService ressursService,
        RessursCommadService ressursCommadService,
        NIKClient nikClient,
        PdlClient pdlClient,
        OrgenhetService orgenhetService,
        RessursOpprettetEmailService ressursOpprettetEmailService
    ) {
        this.ressursService = ressursService;
        this.ressursCommadService = ressursCommadService;
        this.nikClient = nikClient;
        this.pdlClient = pdlClient;
        this.orgenhetService = orgenhetService;
        this.ressursOpprettetEmailService = ressursOpprettetEmailService;
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
        "hasAuthority(T(no.nav.nom.apps.api.common.security.AZPAuthoritiesConverter).AUTH_MUTATE_RESSURS)"
    )
    public ResponseEntity<OpprettRessursResponse> opprettRessurs(
        @RequestBody OpprettRessursRequest request,
        JwtAuthenticationToken token) throws ExecutionException, InterruptedException, TimeoutException {

        var ressurs = ressursService.findByPersonident(request.getPersonident());
        if (ressurs.isPresent()) {
            return ResponseEntity.ok(toResponse(ressurs.get()));
        }

        final var createRessursResponse = createRessurs(request, token);

        try {
            ressursOpprettetEmailService.notifyByEmail(createRessursResponse.navident(), request.getOrgEnhetId());
        } catch (RessursOpprettetEmailException exception) {
            //TODO: consider handling exception if email sending is important
            log.warn("Epost varsling om opprettelse av ressurs til orgenhet leder feilet. {}", exception.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(createRessursResponse));
    }

    private RessursState createRessurs(OpprettRessursRequest request, JwtAuthenticationToken token)
        throws ExecutionException, InterruptedException, TimeoutException {

        final var opprettNavidentResponse = nikClient.opprettNavidentOnBehalfOfUser(
            OpprettNavidentRequest.builder()
                .personid(request.getPersonident())
                .identPrefix(request.getVisningsetternavn().substring(0, 1))
                .identType("STANDARD")
                .build()
        );

        if (!opprettNavidentResponse.getStatusCode().is2xxSuccessful()) {
            throw new ResponseStatusException(
                opprettNavidentResponse.getStatusCode(),
                Optional.ofNullable(opprettNavidentResponse.getBody())
                    .map(OpprettNavidentResponse::getFeilmelding)
                    .orElse("NIK connection error")
            );
        }

        if (opprettNavidentResponse.getBody() != null && opprettNavidentResponse.getBody().getFeilmelding() != null) {
            throw new ResponseStatusException(
                opprettNavidentResponse.getStatusCode(),
                opprettNavidentResponse.getBody().getFeilmelding()
            );
        }

        final var pdlPerson = pdlClient.hentPerson(request.getPersonident());

        if (request.getVisningsmellomnavn() != null && request.getVisningsmellomnavn().isBlank()) {
            request.setVisningsmellomnavn(null);
        }

        log.debug("createRessurs for {} - {}", opprettNavidentResponse.getBody().getNavident(), request.getVisningsetternavn());


        final var opprettRessursCommand = new OpprettRessursCommand(
            request.getPersonident(),
            opprettNavidentResponse.getBody().getNavident(),
            pdlPerson,
            request.getSektor(),
            new Visningsnavn(
                request.getVisningsfornavn(),
                Optional.ofNullable(request.getVisningsmellomnavn()),
                request.getVisningsetternavn()
            ),
            new InputMetadata(
                "GraphQL",
                "opprettRessurs",
                request,
                new Origination(
                    SecurityUtil.getUserFromToken(token).orElseThrow(),
                    SecurityUtil.getSystemFromToken(token).orElseThrow()
                )
            ),
            request.getStartDato(),
            Optional.ofNullable(request.getOrgEnhetId())
                .flatMap(orgenhetService::getById)
                .map(IOrgenhet::getAgressoId)
                .orElse(null), // TODO: Validate not null in RessursCommandService
            request.getAnsettelsesType()
        );

        return ressursCommadService.createRessursSync(opprettRessursCommand);
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
        NoSuchElementException.class
    })
    public ResponseEntity<String> handleBadRequestException(RuntimeException exception) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleBadRequestException(Exception exception) {
        log.error("Intern feil ved opprettelse av ny ressurs", exception);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(exception.getMessage());
    }
}
