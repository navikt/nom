package no.nav.nom.apps.api.ressurs.api.v1.remedy;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.pdl.Person;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonaliaCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonidentCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.rest.dto.NavnDTO;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static no.nav.nom.apps.api.common.AppConfig.SECURE_LOGS;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Tag(name = "Remedy")
@RestController
@RequestMapping("/api/v1/remedy")
public class RemedyController {
    private final Logger log = LoggerFactory.getLogger(RemedyController.class);
    private final RessursStateRepository ressursStateRepository;
    private final PdlClient pdlClient;
    private final NIKClient nikClient;
    private final RessursCommadService ressursCommadService;

    public static final String UNLEASH_HANDLE_PERSONIDENT_CHANGE = "nom.remedy_opprett_ressurs.handle_personident_change";
    private final Unleash unleash;

    public RemedyController(
        RessursStateRepository ressursStateRepository, PdlClient pdlClient, NIKClient nikClient, RessursCommadService ressursCommadService,
        Unleash unleash
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.pdlClient = pdlClient;
        this.nikClient = nikClient;
        this.ressursCommadService = ressursCommadService;
        this.unleash = unleash;
    }

    @PostMapping("/finnRessurs")
    @Operation(
        description = "Personoppslag",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
        }
    )
    @PreAuthorize("hasAuthority('QUERY_PERSON')")
    public ResponseEntity<RessursDTO> finnRessurs(@RequestBody FinnRessursRequest request) {
        return ResponseEntity.ok(finnRessurs(request.getPersonident()));
    }

    public RessursDTO finnRessurs(String personident) {
        log.info(SECURE_LOGS, "Finn ressurs request mottatt:\n{}", personident);

        if (personident == null || personident.isBlank()) {
            log.error("Personident er tom");
            return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Angitt personident er ikke gyldig"
            );
        }
        var response = ressursStateRepository.findByPersonident(personident)
            .map(this::from);

        if (response.isPresent()) {
            log.info(SECURE_LOGS, "Ressurs {} funnet med personident {}", response.get().getNavident(), personident);
            return response.get();
        } else {
            log.debug("Forsøker å finne ressurs i PDL");
            return pdlClient.hentPerson(personident)
                .map(this::from)
                .orElse(null);

        }
    }

    @PostMapping("/opprettRessurs")
    @Transactional
    @Operation(
        description = "Henter en eksisterende eller oppretter en ny navident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @PreAuthorize("hasAuthority('MUTATE_RESSURS')")
    public ResponseEntity<RessursDTO> opprettRessurs(@RequestBody OpprettRessursRequest request) {
        return ResponseEntity.ok(tryOpprettRessurs(request));
    }

    public RessursDTO tryOpprettRessurs(OpprettRessursRequest request) {
        log.info(SECURE_LOGS, "Opprett ressurs request mottatt:\n{}", request);
        var funnetRessurs = ressursStateRepository.findByPersonident(request.getPersonident());

        if (funnetRessurs.isPresent()) {
            log.debug(SECURE_LOGS, "Ressurs med personident {} finnes fra før", request.getPersonident());
            return funnetRessurs.map(this::from).get();
        }

        String opprettetNavident;
        try {
            final var nikResponse = nikClient.opprettNavident(
                OpprettNavidentRequest.builder()
                    .personid(request.getPersonident())
                    .identPrefix(request.getForbokstav())
                    .identType(request.getIdentType())
                    .build()
            );

            if (!nikResponse.getStatusCode().is2xxSuccessful()) {
                return createErrorResponse(
                    nikResponse.getStatusCode(),
                    nikResponse.hasBody() ? nikResponse.getBody().getFeilmelding() : "Ukjent feil har oppstått under opprettelse av navident"
                );
            }

            opprettetNavident = nikResponse.getBody().getNavident();

        } catch (Exception exception) {
            log.warn("Unhandled exception during call to nav-ident: " + exception.getMessage(), exception);
            return createErrorResponse(INTERNAL_SERVER_ERROR, "Intern feil inntraff");
        }

        InputMetadata metadataFromRequest = new InputMetadata(
            "REST",
            "/api/v1/remedy/ressurs",
            request,
            new Origination(request.getSubmitter(), SecurityUtil.getAuthenticatedAzpName())
        );

        if (unleash.isEnabled(UNLEASH_HANDLE_PERSONIDENT_CHANGE)) {
            // Det kan være at identen person har byttet f.nr. i såfall har nav-ident returnert eksisterende navident for personen
            funnetRessurs = ressursStateRepository.findByNavident(opprettetNavident);
            if (funnetRessurs.isPresent()) {
                log.debug("Ressurs funnet med navident {}", opprettetNavident);

                // Endre personident på eksisterende ressurs
                var eksisterendeRessurs = funnetRessurs.get();

                ressursCommadService.oppdaterPersonidentCommand(
                    new OppdaterPersonidentCommand(
                        eksisterendeRessurs.fid(),
                        request.getPersonident()
                    ),
                    metadataFromRequest
                );

                // Hvis personalia/visningsnavn er endret, gjør en endre personalia også.
                if (!visningsnavnEquals(request, eksisterendeRessurs)) {
                    ressursCommadService.oppdaterPersonaliaEvent(
                        new OppdaterPersonaliaCommand(
                            eksisterendeRessurs.fid(),
                            new Visningsnavn(request.getFornavn(), Optional.ofNullable(request.getMellomnavn()), request.getEtternavn())
                        ),
                        metadataFromRequest
                    );
                }
                return RessursDTO.builder()
                    .personident(request.getPersonident())
                    .navident(eksisterendeRessurs.navident())
                    .navn(NavnDTO.builder()
                        .fornavn(request.getFornavn())
                        .mellomnavn(request.getMellomnavn())
                        .etternavn(request.getEtternavn())
                        .build())
                    .build();
            }
        }

        Optional<Person> pdlPerson = pdlClient.hentPerson(request.getPersonident());

        try {
            final var opprettRessursCommand = new OpprettRessursCommand(
                request.getPersonident(),
                opprettetNavident,
                pdlPerson,
                Sektor.valueOf(request.getSektor()),
                new Visningsnavn(request.getFornavn(), Optional.ofNullable(request.getMellomnavn()), request.getEtternavn()),
                metadataFromRequest,
                null,
                null,
                null
            );

            return from(ressursCommadService.createRessursSync(opprettRessursCommand));
        } catch (Exception exception) {
            log.warn("Unhandled exception during creation of ressurs: " + exception.getMessage(), exception);
            return createErrorResponse(INTERNAL_SERVER_ERROR, "Intern feil inntraff");
        }
    }

    private boolean visningsnavnEquals(OpprettRessursRequest request, RessursState eksisterendeRessurs) {
        return eksisterendeRessurs.visningsnavn().fornavn().equals(request.getFornavn()) &&
            eksisterendeRessurs.visningsnavn().etternavn().equals(request.getEtternavn()) &&
            (eksisterendeRessurs.visningsnavn().mellomnavn().isEmpty() && request.getMellomnavn() == null ||
                eksisterendeRessurs.visningsnavn().mellomnavn().get().equals(request.getMellomnavn()));
    }

    private RessursDTO createErrorResponse(HttpStatusCode httpStatusCode, String feilmelding) {
        log.error("Feilhåndtering i kall fra Remedy - Returnerer feil '{} - {}'", httpStatusCode, feilmelding);
        return RessursDTO.builder()
            .status(String.valueOf(httpStatusCode.value()))
            .feilmelding(feilmelding)
            .build();
    }

    private RessursDTO from(RessursState state) {
        return RessursDTO.builder()
            .status(Integer.toString(HttpStatus.OK.value()))
            .personident(state.personident())
            .navident(state.navident())
            .navn(NavnDTO.builder()
                .fornavn(state.visningsnavn().fornavn())
                .mellomnavn(state.visningsnavn().mellomnavn().orElse(null))
                .etternavn(state.visningsnavn().etternavn())
                .build())
            .build();
    }

    private RessursDTO from(Person person) {
        return RessursDTO.builder()
            .status(Integer.toString(HttpStatus.OK.value()))
            .navn(NavnDTO.builder()
                .fornavn(person.getFornavn())
                .mellomnavn(person.getMellomnavn())
                .etternavn(person.getEtternavn())
                .build())
            .build();
    }
}
