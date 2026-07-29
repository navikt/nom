package no.nav.nom.apps.api.ressurs.api.v1.navident;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.pdl.PdlClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static no.nav.nom.apps.api.common.AppConfig.TEAM_LOGS;

@Slf4j
@Tag(name = "Navident")
@RestController
@RequestMapping(NavidentController.BASE_URL)
@PreAuthorize("hasAuthority('ADMIN') || hasAuthority('NOM_WRITE_EXTERNAL_FUNDS_RECIPIENT')")
public class NavidentController {

    // TODO: Consider using RessursResolver methods to avoid duplicating code in here.
    public static final String BASE_URL = "/api/v1/navident";
    private final NIKClient nikClient;
    private final PdlClient pdlClient;

    public NavidentController(
        NIKClient nikClient, PdlClient pdlClient
    ) {
        this.nikClient = nikClient;
        this.pdlClient = pdlClient;
    }

    @ExceptionHandler({AccessDeniedException.class})
    @ResponseBody
    public ResponseEntity<NavidentResponse> handleAccessDeniedException() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    @Transactional
    @Operation(
        description = "Henter en eksisterende navident, eller oppretter en ny navident for gitt FNR",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "404", description = "Failure, body == null"),
        @ApiResponse(responseCode = "500", description = "Failure, body == null")
    })
    public ResponseEntity<NavidentResponse> opprettStandardNavident(@RequestBody NavidentRequest request) {

        log.info(TEAM_LOGS, "opprettNavident kalt for personIdent {}", request.getPersonident());

        var maybePerson = pdlClient.hentPerson(request.getPersonident());
        if (maybePerson.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        var person = maybePerson.get();
        var etternavnPrefixBokstav = person.getEtternavn().substring(0, 1).toUpperCase();

        var opprettNavidentRequest = OpprettNavidentRequest.builder()
            .personid(request.getPersonident())
            .identPrefix(etternavnPrefixBokstav)
            .identType(request.getIdentType().name())
            .build();

        var nikResponse = nikClient.opprettNavident(opprettNavidentRequest);
        var successfulResponse = nikResponse.getStatusCode().is2xxSuccessful();
        var nikResponseBody = nikResponse.getBody();
        if (!successfulResponse || nikResponseBody == null) {
            log.info(TEAM_LOGS, "opprettNavident feilet for personIdent {} med http-status {}", request.getPersonident(), nikResponse.getStatusCode());
            log.info("opprettNavident feilet med http-status {}", nikResponse.getStatusCode());
            return ResponseEntity.internalServerError().build();
        }

        var feilmelding = nikResponseBody.getFeilmelding();
        var hentetNavident = nikResponseBody.getNavident();
        if (feilmelding != null) {
            log.info(TEAM_LOGS, "Feilmelding fra NIK for personIdent {}:{}", request.getPersonident(), feilmelding);
        }

        var internFeilmelding = feilmelding != null || hentetNavident == null;
        if (internFeilmelding) {
            log.info(TEAM_LOGS, "opprettNavident feilet for personIdent {}", request.getPersonident());
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok(NavidentResponse.builder().navident(hentetNavident).build());
    }
}
