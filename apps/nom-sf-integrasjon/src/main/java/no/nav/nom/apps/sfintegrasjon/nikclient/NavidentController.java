package no.nav.nom.apps.sfintegrasjon.nikclient;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.sfintegrasjon.common.SwaggerConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
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
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created")
    })
    @PostMapping("/create")
    public ResponseEntity<OpprettNavidentResponse> opprettNavident(@RequestBody OpprettNavidentRequest request) {
        if (unleash.isEnabled("nom-sf-integrasjon.navident-create")) {
            return nikClient.opprettNavident(request);
        } else {
            return new ResponseEntity<>(new OpprettNavidentResponse("A000001", "Unleash toggle er av"), HttpStatus.OK);
        }
    }
}
