package no.nav.nom.apps.api.rest.navident;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.eventsourcing.state.RessursStateService;
import no.nav.nom.apps.api.nik.NIKConsumer;
import no.nav.nom.apps.api.pdl.Kjonn;
import no.nav.nom.apps.api.pdl.PdlConsumer;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Navident")
@RestController
@RequestMapping(NavidentController.BASE_URL)
public class NavidentController {

    // TODO: Consider using RessursResolver methods to avoid duplicating code in here.
    public static final String BASE_URL = "/api/v1/navident";
    private final RessursCommadService ressursCommadService;
    private final RessursStateService ressursStateService;
    private final NIKConsumer nikConsumer;
    private final PdlConsumer pdlConsumer;
    private final ObjectMapper mapper;

    public NavidentController(
            RessursCommadService ressursCommadService, RessursStateService ressursStateService,
            NIKConsumer nikConsumer,
            PdlConsumer pdlConsumer,
            ObjectMapper mapper
    ) {
        this.ressursCommadService = ressursCommadService;
        this.ressursStateService = ressursStateService;
        this.nikConsumer = nikConsumer;
        this.mapper = mapper;
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
    @ApiResponses(
            value = @ApiResponse(
                    responseCode = "200",
                    description = "Success"
            )
    )
    @PreAuthorize("hasAuthority(T(no.nav.nom.apps.api.common.security.AZPAuthoritiesConverter).AUTH_QUERY_PERSON)")
    public ResponseEntity<NavidentResponse> opprettNavident(
            @RequestBody NavidentRequest request,
            @AuthenticationPrincipal JwtAuthenticationToken token
    ) {
        final var responseBody = NavidentResponse.builder()
                .status(Integer.toString(HttpStatus.OK.value()));

        final var ressurs = ressursStateService.findByPersonident(request.getPersonident()).orElseGet(() -> {
            try {
                final var nikResponse = nikConsumer.opprettNavident(request.getPersonident(), request.getForbokstav(), request.getIdentType(), false);

                if (nikResponse.getStatusCode().is2xxSuccessful()) {
                    final var opprettRessursCommand = new OpprettRessursCommand(
                            request.getPersonident(),
                            nikResponse.getBody().getNavident(),
                            RessursOpprettetEventV1.RessursRegisteredEventData.Sektor.valueOf(request.getSektor()),
                            new RessursOpprettetEventV1.RessursRegisteredEventData.Visningsnavn(
                                    request.getFornavn(),
                                    request.getMellomnavn(),
                                    request.getEtternavn()
                            )
                    );

                    responseBody.status(String.valueOf(nikResponse.getStatusCode().value()));

                    return ressursCommadService.createRessursSync(opprettRessursCommand, null);
                }

                responseBody.status(String.valueOf(nikResponse.getStatusCode().value())).feilmelding(nikResponse.getBody().getFeilmelding());
            } catch (Exception e) {
                log.info(e.getCause().getMessage());
                responseBody.status(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value())).feilmelding("Intern feil inntraff");
            }

            return null;
        });

        if (ressurs != null) {
            responseBody
                    .personident(ressurs.personident())
                    .navident(ressurs.navident())
                    .fornavn(ressurs.visningsnavn().fornavn())
                    .mellomnavn(ressurs.visningsnavn().mellomnavn().orElse(null))
                    .etternavn(ressurs.visningsnavn().etternavn());
        }

        return ResponseEntity.ok(responseBody.build());
    }

    private String getKjonn(String personident, String identType) {
        if (!identType.equals("STANDARD")) {
            return Kjonn.UKJENT.name();
        }

        return pdlConsumer.hentPerson(personident)
                .map(pers -> pers.getKjonn().name())
                .orElseGet(Kjonn.UKJENT::name);
    }
}
