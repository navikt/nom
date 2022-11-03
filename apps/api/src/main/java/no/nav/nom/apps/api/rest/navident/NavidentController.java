package no.nav.nom.apps.api.rest.navident;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadata;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
import no.nav.nom.apps.api.nik.NIKConsumer;
import no.nav.nom.apps.api.pdl.Kjonn;
import no.nav.nom.apps.api.pdl.PdlConsumer;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.apps.api.ressurs.implementations.postgresql.EventBasedRessurs;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "Navident")
@RestController
@RequestMapping(NavidentController.BASE_URL)
public class NavidentController {

    // TODO: Consider using RessursResolver methods to avoid duplicating code in here.
    public static final String BASE_URL = "/api/v1/navident";

    private final IRessursService ressursService;
    private final NIKConsumer nikConsumer;
    private final PdlConsumer pdlConsumer;
    private final ObjectMapper mapper;

    public NavidentController(
            IRessursService ressursService,
            NIKConsumer nikConsumer,
            PdlConsumer pdlConsumer,
            ObjectMapper mapper
    ) {
        this.nikConsumer = nikConsumer;
        this.mapper = mapper;
        this.ressursService = ressursService;
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
            @NonNull JwtAuthenticationToken token
    ) {
        final var responseBody = NavidentResponse.builder()
                .status(Integer.toString(HttpStatus.OK.value()));

        final var ressurs = ressursService.findByPersonIdent(request.getPersonident()).orElseGet(() -> {
            try {
                final var nikResponse = nikConsumer.opprettNavident(request.getPersonident(), request.getForbokstav(), request.getIdentType(), false);

                if (nikResponse.getStatusCode().is2xxSuccessful()) {
                    Map<String, Object> data = new HashMap<>();

                    data.put(EventBasedRessurs.PERSONIDENT_FIELD, request.getPersonident());
                    data.put(EventBasedRessurs.NAVIDENT_FIELD, nikResponse.getBody().getNavident());
                    data.put(EventBasedRessurs.VISNINGSFORNAVN_FIELD, request.getFornavn());
                    data.put(EventBasedRessurs.VISNINGSMELLOMNAVN_FIELD, request.getMellomnavn());
                    data.put(EventBasedRessurs.VISNINGSETTERNAVN_FIELD, request.getEtternavn());
                    data.put(EventBasedRessurs.SEKTOR_FIELD, request.getSektor());
                    data.put(EventBasedRessurs.KJONN_FIELD, getKjonn(request.getPersonident(), request.getIdentType()));

                    final var metadata = HendelseMetadataSerialize.builder()
                            .opprettetAv(new HendelseMetadata.OpprettetAv(token))
                            .opprettetTidspunkt(LocalDateTime.now())
                            .rawInputData(mapper.valueToTree(data))
                            .build();

                    responseBody.status(String.valueOf(nikResponse.getStatusCode().value()));

                    return ressursService.opprettRessurs(request.getPersonident(), data, metadata);
                }

                responseBody.status(String.valueOf(nikResponse.getStatusCode().value())).feilmelding(nikResponse.getBody().getFeilmelding());
            } catch (Exception exception) {
                log.info("Unhandled exception during creation of navident: " + exception.getMessage(), exception);
                responseBody.status(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value())).feilmelding("Intern feil inntraff");
            }

            return null;
        });

        if (ressurs != null) {
            responseBody
                    .personident(ressurs.getPersonIdent())
                    .navident(ressurs.getNavIdent())
                    .fornavn(ressurs.getVisningsfornavn().orElse(null))
                    .mellomnavn(ressurs.getVisningsmellomnavn().orElse(null))
                    .etternavn(ressurs.getVisningsetternavn().orElse(null));
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
