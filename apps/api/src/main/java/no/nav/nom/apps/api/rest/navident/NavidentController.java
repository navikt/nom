package no.nav.nom.apps.api.rest.navident;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadata;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
import no.nav.nom.apps.api.nik.NIKConsumer;
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

@Tag(name = "Navident")
@RestController
@RequestMapping(NavidentController.BASE_URL)
@RequiredArgsConstructor
public class NavidentController {

    private final NIKConsumer nikConsumer;
    private final TokenRepository tokenRepository;
    private final AppProperties appProperties;
    private final ObjectMapper mapper;
    private final IRessursService ressursService;

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
            return ResponseEntity.ok(NavidentResponse.builder().status(HttpStatus.FORBIDDEN).feilmelding("Forbidden").build());
        }

        try {
            val aadToken = tokenRepository.getAadToken(appProperties.getNik().getAadClientId());
            val nikResponse = nikConsumer.opprettNavident(request.getPersonident(), request.getForbokstav(), request.getSektor(), aadToken.serialize());

            if (List.of(HttpStatus.OK, HttpStatus.CREATED).contains(nikResponse.getStatusCode())) {

                val data = mapper.createObjectNode()
                        .put(EventBasedRessurs.PERSONIDENT_FIELD, request.getPersonident())
                        .put(EventBasedRessurs.NAVIDENT_FIELD, nikResponse.getBody().getNavident())
                        .put(EventBasedRessurs.VISNINGSFORNAVN_FIELD, request.getFornavn())
                        .put(EventBasedRessurs.VISNINGSETTERNAVN_FIELD, request.getEtternavn())
                        .put(EventBasedRessurs.SEKTOR_FIELD, request.getSektor());
                val metadata = HendelseMetadataSerialize.builder()
                        .opprettetAv(new HendelseMetadata.OpprettetAv(token))
                        .opprettetTidspunkt(LocalDateTime.now())
                        .rawInputData(data)
                        .build();

                ressursService.opprettRessurs0(request.getPersonident(), data, metadata);

                return ResponseEntity.ok(NavidentResponse.builder()
                        .navident(nikResponse.getBody().getNavident())
                        .status(nikResponse.getStatusCode())
                        .build());
            }

            return ResponseEntity.ok(NavidentResponse.builder()
                    .status(nikResponse.getStatusCode())
                    .feilmelding(nikResponse.getBody().getFeilmelding())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(NavidentResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .feilmelding("Intern feil inntraff")
                    .build());
        }
    }
}
