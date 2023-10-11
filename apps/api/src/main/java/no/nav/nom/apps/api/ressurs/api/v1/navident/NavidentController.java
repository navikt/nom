package no.nav.nom.apps.api.ressurs.api.v1.navident;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.pdl.Person;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.ressurs.api.v1.remedy.OpprettRessursRequest;
import no.nav.nom.apps.api.ressurs.api.v1.remedy.RemedyController;
import no.nav.nom.apps.api.ressurs.api.v1.remedy.RessursDTO;
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
    private final NIKClient nikClient;
    private final PdlClient pdlClient;
    public static final String UNLEASH_REDIRECT_TIL_REMEDY_CONTROLLER = "nom.remedy_redirect_til_remedycontroller";
    private final Unleash unleash;
    private final RemedyController remedyController;


    public NavidentController(
        RessursCommadService ressursCommadService,
        IRessursService ressursService,
        NIKClient nikClient,
        PdlClient pdlClient,
        Unleash unleash,
        RemedyController remedyController
    ) {
        this.ressursCommadService = ressursCommadService;
        this.ressursService = ressursService;
        this.nikClient = nikClient;
        this.pdlClient = pdlClient;
        this.unleash = unleash;
        this.remedyController = remedyController;
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
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Success")})
    @PreAuthorize("hasAuthority('QUERY_PERSON')")
    public ResponseEntity<NavidentResponse> opprettNavident(@RequestBody NavidentRequest request, @NonNull JwtAuthenticationToken token) {
        if (unleash.isEnabled(UNLEASH_REDIRECT_TIL_REMEDY_CONTROLLER)) {
            return ResponseEntity.ok(toNavidentResponse(remedyController.opprettRessurs(
                convert(request),
                SecurityUtil.getSystemFromToken(token).orElseThrow()
            )));
        }

        final var funnetRessurs = ressursService.findByPersonident(request.getPersonident());

        if (funnetRessurs.isPresent()) {
            return funnetRessurs.map(this::toNavidentResponse)
                .map(ResponseEntity::ok)
                .get();
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

        Optional<Person> pdlPerson = pdlClient.hentPerson(request.getPersonident());

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
                null,
                null
            );

            return ResponseEntity.ok(toNavidentResponse(ressursCommadService.createRessursSync(opprettRessursCommand)));
        } catch (Exception exception) {
            log.warn("Unhandled exception during creation of ressurs: " + exception.getMessage(), exception);
            return ResponseEntity.ok(NavidentResponse.medFeilmelding(INTERNAL_SERVER_ERROR, "Intern feil inntraff"));
        }
    }

    private OpprettRessursRequest convert(NavidentRequest request) {
        return OpprettRessursRequest.builder()
            .personident(request.getPersonident())
            .fornavn(request.getFornavn())
            .mellomnavn(request.getMellomnavn())
            .etternavn(request.getEtternavn())
            .forbokstav(request.getForbokstav())
            .identType(request.getIdentType())
            .sektor(request.getSektor())
            .submitter(request.getSubmitter())
            .build();
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

    private NavidentResponse toNavidentResponse(RessursDTO ressurs) {
        return NavidentResponse.builder()
            .status(Integer.toString(HttpStatus.OK.value()))
            .personident(ressurs.getPersonident())
            .navident(ressurs.getNavident())
            .fornavn(ressurs.getNavn().getFornavn())
            .mellomnavn(ressurs.getNavn().getMellomnavn())
            .etternavn(ressurs.getNavn().getEtternavn())
            .build();
    }
}
