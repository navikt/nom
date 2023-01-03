package no.nav.nom.apps.api.rest.navident;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@Tag(name = "Navident")
@RestController
@RequestMapping(NavidentController.BASE_URL)
public class NavidentController {

    // TODO: Consider using RessursResolver methods to avoid duplicating code in here.
    public static final String BASE_URL = "/api/v1/navident";
    private final RessursCommadService ressursCommadService;
    private final IRessursService ressursService;
    private final NIKConsumer nikConsumer;
    private final PdlConsumer pdlConsumer;

    public NavidentController(
            RessursCommadService ressursCommadService,
            IRessursService ressursService,
            NIKConsumer nikConsumer,
            PdlConsumer pdlConsumer
    ) {
        this.ressursCommadService = ressursCommadService;
        this.ressursService = ressursService;
        this.nikConsumer = nikConsumer;
        this.pdlConsumer = pdlConsumer;
    }


    @ExceptionHandler({AccessDeniedException.class})
    @ResponseBody
    public ResponseEntity<NavidentResponse> handleAccessDeniedException() {
        return ResponseEntity.ok(
                NavidentResponse.builder()
                        .status(String.valueOf(HttpStatus.FORBIDDEN.value()))
                        .feilmelding("Forbidden")
                        .build()
        );
    }

    @PostMapping
    @Transactional
    @Operation(
            description = "Henter en eksisterende eller oppretter en ny navident",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success")
    })
    @PreAuthorize("hasAuthority(T(no.nav.nom.apps.api.common.security.AZPAuthoritiesConverter).AUTH_QUERY_PERSON)")
    public ResponseEntity<NavidentResponse> opprettNavident(@RequestBody NavidentRequest request, @NonNull JwtAuthenticationToken token) {
        final var funnetRessurs = ressursService.findByPersonident(request.getPersonident());

        if (funnetRessurs.isPresent()) {
            return funnetRessurs.map(this::toNavidentResponse)
                    .map(ResponseEntity::ok)
                    .get();
        }

        String opprettetNavident;
        try {
            final var nikResponse = nikConsumer.opprettNavident(request.getPersonident(), request.getForbokstav(), request.getIdentType(), false);

            if (!nikResponse.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(
                        NavidentResponse.medFeilmelding(
                                nikResponse.getStatusCode(),
                                nikResponse.hasBody() ? nikResponse.getBody().getFeilmelding() : "Ukjent feil har oppstått under opprettelse av navident"
                        )
                );
            }

            opprettetNavident = nikResponse.getBody().getNavident();

        } catch (Exception exception) {
            log.warn("Unhandled exception during call to nav-ident: " + exception.getMessage(), exception);
            return ResponseEntity.ok(NavidentResponse.medFeilmelding(INTERNAL_SERVER_ERROR, "Intern feil inntraff"));
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
                            "/api/v1/navident",
                            request,
                            new Origination(request.getSubmitter(), SecurityUtil.getSystemFromToken(token).orElseThrow())
                    ),
                    null,
                    null
            );

            return ResponseEntity.ok(toNavidentResponse(ressursCommadService.createRessursSync(opprettRessursCommand)));
        } catch (Exception exception) {
            log.warn("Unhandled exception during creation of ressurs: " + exception.getMessage(), exception);
            return ResponseEntity.ok(NavidentResponse.medFeilmelding(INTERNAL_SERVER_ERROR, "Intern feil inntraff"));
        }
    }

    private NavidentResponse toNavidentResponse(RessursState ressurs) {
        return NavidentResponse.builder()
                .status(Integer.toString(HttpStatus.OK.value()))
                .personident(ressurs.personident())
                .navident(ressurs.navident())
                .fornavn(ressurs.visningsnavn().fornavn())
                .mellomnavn(ressurs.visningsnavn().mellomnavn().orElse(null))
                .etternavn(ressurs.visningsnavn().etternavn())
                .build();
    }
}
