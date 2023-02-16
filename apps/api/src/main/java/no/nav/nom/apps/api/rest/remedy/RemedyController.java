package no.nav.nom.apps.api.rest.remedy;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.nik.NIKConsumer;
import no.nav.nom.apps.api.pdl.PdlConsumer;
import no.nav.nom.apps.api.pdl.Person;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Tag(name = "Remedy")
@RestController
@RequestMapping("/api/v1/remedy")
public class RemedyController {

    private final Logger log = LoggerFactory.getLogger(RemedyController.class);


    private final IRessursService ressursService;
    private final PdlConsumer pdlConsumer;
    private final NIKConsumer nikConsumer;
    private final RessursCommadService ressursCommadService;

    public RemedyController(IRessursService ressursService, PdlConsumer pdlConsumer, NIKConsumer nikConsumer, RessursCommadService ressursCommadService) {
        this.ressursService = ressursService;
        this.pdlConsumer = pdlConsumer;
        this.nikConsumer = nikConsumer;
        this.ressursCommadService = ressursCommadService;
    }

    @GetMapping("/ressurs/{personident}")
    @Operation(
        description = "Personoppslag",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
    })
    @PreAuthorize("hasAuthority(T(no.nav.nom.apps.api.common.security.AZPAuthoritiesConverter).AUTH_QUERY_PERSON)")
    public ResponseEntity<RessursDTO> finnRessurs(@PathVariable("personident") String personident) {

        var response = ressursService.findByPersonident(personident)
            .map(this::mapFrom);

        if (response.isPresent()) {
            return ResponseEntity.ok(response.get());
        } else {
            log.debug("Forsøker å finne ressurs i PDL");
            return pdlConsumer.hentPerson(personident)
                .map(this::mapFrom)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(createErrorResponse(
                    HttpStatus.NOT_FOUND,
                    "Kunne ikke finne ressurs med gitt personident"
                )));
        }
    }


    @PostMapping("/ressurs")
    @Transactional
    @Operation(
        description = "Henter en eksisterende eller oppretter en ny navident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success")
    })
    @PreAuthorize("hasAuthority(T(no.nav.nom.apps.api.common.security.AZPAuthoritiesConverter).AUTH_QUERY_PERSON)")
    public ResponseEntity<RessursDTO> opprettRessurs(@RequestBody OpprettRessursRequest request, @NonNull JwtAuthenticationToken token) {

        final var funnetRessurs = ressursService.findByPersonident(request.getPersonident());

        if (funnetRessurs.isPresent()) {
            return funnetRessurs.map(this::mapFrom)
                .map(ResponseEntity::ok)
                .get();
        }

        String opprettetNavident;
        try {
            final var nikResponse = nikConsumer.opprettNavident(request.getPersonident(), request.getForbokstav(), request.getIdentType(), false);

            if (!nikResponse.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(
                    createErrorResponse(
                        nikResponse.getStatusCode(),
                        nikResponse.hasBody() ? nikResponse.getBody().getFeilmelding() : "Ukjent feil har oppstått under opprettelse av navident"
                    )
                );
            }

            opprettetNavident = nikResponse.getBody().getNavident();

        } catch (Exception exception) {
            log.warn("Unhandled exception during call to nav-ident: " + exception.getMessage(), exception);
            return ResponseEntity.ok(createErrorResponse(INTERNAL_SERVER_ERROR, "Intern feil inntraff"));
        }

        Optional<Person> pdlPerson = pdlConsumer.hentPerson(request.getPersonident());

        try {
            final var opprettRessursCommand = new OpprettRessursCommand(
                request.getPersonident(),
                opprettetNavident,
                pdlPerson,
                Sektor.valueOf(request.getSektor()),
                new Visningsnavn(request.getFornavn(), Optional.ofNullable(request.getMellomnavn()), request.getEtternavn()),
                new InputMetadata(
                    "REST",
                    "/api/v1/remedy/ressurs",
                    request,
                    new Origination(request.getSubmitter(), SecurityUtil.getSystemFromToken(token).orElseThrow())
                ),
                null,
                null
            );

            return ResponseEntity.ok(mapFrom(ressursCommadService.createRessursSync(opprettRessursCommand)));
        } catch (Exception exception) {
            log.warn("Unhandled exception during creation of ressurs: " + exception.getMessage(), exception);
            return ResponseEntity.ok(createErrorResponse(INTERNAL_SERVER_ERROR, "Intern feil inntraff"));
        }
    }

    private RessursDTO createErrorResponse(HttpStatus status, String feilmelding) {
        return RessursDTO.builder()
            .status(status.toString())
            .feilmelding(feilmelding)
            .build();
    }

    private RessursDTO mapFrom(RessursState state) {
        return RessursDTO.builder()
            .navn(RessursDTO.Navn.builder()
                .fornavn(state.visningsnavn().fornavn())
                .mellomnavn(state.visningsnavn().mellomnavn().orElse(null))
                .etternavn(state.visningsnavn().etternavn())
                .build())
            .build();
    }

    private RessursDTO mapFrom(Person person) {
        return RessursDTO.builder()
            .navn(RessursDTO.Navn.builder()
                .fornavn(person.getFornavn())
                .mellomnavn(person.getMellomnavn())
                .etternavn(person.getEtternavn())
                .build())
            .build();
    }
}
