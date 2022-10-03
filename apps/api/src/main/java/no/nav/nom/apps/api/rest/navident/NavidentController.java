package no.nav.nom.apps.api.rest.navident;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadata;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
import no.nav.nom.apps.api.nik.NIKConsumer;
import no.nav.nom.apps.api.pdl.Kjonn;
import no.nav.nom.apps.api.pdl.PdlConsumer;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.apps.api.ressurs.implementations.postgresql.EventBasedRessurs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Tag(name = "Navident")
@RestController
@RequestMapping(NavidentController.BASE_URL)
@RequiredArgsConstructor
public class NavidentController {

    private final NIKConsumer nikConsumer;
    private final ObjectMapper mapper;
    private final IRessursService ressursService;
    private final PdlConsumer pdlConsumer;

    @Value("${app.graphql.preauth.query.person}")
    private List<String> preAuth;

    public static final String BASE_URL = "api/v1/navident";


    @PostMapping
    @Transactional
    @Operation(
            description = "Henter en eksisterende eller oppretter en ny navident",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN))
    @ApiResponses(value = @ApiResponse(responseCode = "200", description = "Success"))
    public ResponseEntity<NavidentResponse> opprettNavident(@RequestBody NavidentRequest request) {
        val token = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        if (token == null || !preAuth.contains(token.getTokenAttributes().get("azp"))) {
            return ResponseEntity.ok(NavidentResponse.builder().status(String.valueOf(HttpStatus.FORBIDDEN.value())).feilmelding("Forbidden").build());
        }

        val responseBody = NavidentResponse.builder()
                .status(Integer.toString(HttpStatus.OK.value()));

        val ressurs = ressursService.findByPersonIdent(request.getPersonident()).orElseGet(() -> {
            try {
                val nikResponse = nikConsumer.opprettNavident(request.getPersonident(), request.getForbokstav(), request.getIdentType(), false);

                if (List.of(HttpStatus.OK, HttpStatus.CREATED).contains(nikResponse.getStatusCode())) {
                    val data = mapper.createObjectNode()
                            .put(EventBasedRessurs.PERSONIDENT_FIELD, request.getPersonident())
                            .put(EventBasedRessurs.NAVIDENT_FIELD, nikResponse.getBody().getNavident())
                            .put(EventBasedRessurs.VISNINGSFORNAVN_FIELD, request.getFornavn())
                            .put(EventBasedRessurs.VISNINGSMELLOMNAVN_FIELD, request.getMellomnavn())
                            .put(EventBasedRessurs.VISNINGSETTERNAVN_FIELD, request.getEtternavn())
                            .put(EventBasedRessurs.SEKTOR_FIELD, request.getSektor())
                            .put(EventBasedRessurs.KJONN_FIELD, getKjonn(request.getPersonident(), request.getIdentType()));
                    val metadata = HendelseMetadataSerialize.builder()
                            .opprettetAv(new HendelseMetadata.OpprettetAv(token))
                            .opprettetTidspunkt(LocalDateTime.now())
                            .rawInputData(data)
                            .build();

                    responseBody.status(String.valueOf(nikResponse.getStatusCode().value()));

                    return ressursService.opprettRessurs(request.getPersonident(), data, metadata);
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

        val person = pdlConsumer.hentPerson(personident).orElse(null);

        return person != null ? person.getKjonn().name() : Kjonn.UKJENT.name();
    }
}
