package no.nav.nom.apps.sfintegrasjon.nikclient;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.sfintegrasjon.common.StandardResponse;
import no.nav.nom.apps.sfintegrasjon.config.OpenApiConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "NIK API", description = "Endpoints for SAP SuccessFactors to create navident from NIK.")
@PreAuthorize("hasAnyAuthority('ZALARIS', 'ADMIN')")
@RestController
@RequestMapping("/api/v1/zalaris/nik")
public class NavidentController {

    private final NIKClient nikClient;
    private final Unleash unleash;

    public NavidentController(NIKClient nikClient, Unleash unleash) {
        this.nikClient = nikClient;
        this.unleash = unleash;
    }

    @Operation(
        description = "Create a navident in NIK, ",
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created")
    })
    @PostMapping("/create")
    public ResponseEntity<OpprettNavidentResponse> opprettNavident(@RequestBody OpprettNavidentRequest request) {
        if (unleash.isEnabled("nom-sf-integrasjon.navident-create")) {
            log.info("Forsøker å opprette navident fra SF {}", request);
            return nikClient.opprettNavident(request);
        } else {
            return new ResponseEntity<>(new OpprettNavidentResponse("A000001", "Unleash toggle er av"), HttpStatus.OK);
        }
    }

    @ExceptionHandler({
        Exception.class
    })
    public ResponseEntity<StandardResponse> handleException(Exception e) {
        log.error(e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder().message(String.format("Unhandled server error: %s", e.getMessage())).build());
    }
}
