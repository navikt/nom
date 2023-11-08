package no.nav.nom.apps.navident.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.val;
import no.nav.nom.apps.navident.IdentService;
import no.nav.nom.apps.navident.api.dto.NavidentRequestDTO;
import no.nav.nom.apps.navident.api.dto.NavidentResponseDTO;
import no.nav.nom.apps.navident.common.LogConfig;
import no.nav.nom.apps.navident.common.SwaggerConfig;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.eventsource.EventSourceProducer;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.libs.utils.common.mdc.MdcConfig;
import org.flywaydb.core.internal.util.HtmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@Tag(name = "Navident")
@RestController
@RequestMapping(NavidentController.BASE_URL)
public class NavidentController {
    private final Map<String, Navident> navidenter;
    public final static String BASE_URL = "/navident";
    private final static Logger logger = LoggerFactory.getLogger(NavidentController.class);
    private final IdentService identService;
    private final EventSourceProducer eventSourceProducer;

    public NavidentController(Map<String, Navident> navidenter, IdentService identService, EventSourceProducer eventSourceProducer) {
        this.navidenter = navidenter;
        this.identService = identService;
        this.eventSourceProducer = eventSourceProducer;
    }

    @Operation(
            description = "Bestill en nav-ident",
            security = @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_CONFIGURATION_NAME)
    )
    @Parameters(
            {
                    @Parameter(ref = "#/components/parameters/" + MdcConfig.HEADER_CALL_ID_KEY),
                    @Parameter(ref = "#/components/parameters/" + MdcConfig.HEADER_X_REQUEST_ID_KEY),
                    @Parameter(ref = "#/components/parameters/" + MdcConfig.HEADER_CONSUMER_ID_KEY)
            }
    )
    @ApiResponses(
            {
                    @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
                    @ApiResponse(responseCode = "400", description = "Bad request, failure to supply a required parameter"),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized, bearer token not supplied or invalid",
                            content = @Content, // Empty response
                            headers = {
                                    @Header(name = "www-authenticate", description = "Error message from bearer/jwt authentication process")
                            }
                    )
            }
    )
    @PostMapping(value = "/create")
    public ResponseEntity<NavidentResponseDTO> createNavident(@RequestBody NavidentRequestDTO request) {
        logger.info(LogConfig.SECURE_LOGS, "Navident bestilt for: {}", request);
        val bestilling = identService.bestillIdent(request.getPersonid(), request.getIdentType(), request.getIdentPrefix().charAt(0));
        val response = NavidentResponseDTO.builder()
                .navident(bestilling.getBody().getNavident())
                .feilmelding(bestilling.getBody().getFeilmelding())
                .build();

        return ResponseEntity.status(bestilling.getHttpStatus()).body(response);
    }

    @Operation(
        description = "Slett en nav-ident - navidenten vil anses som ugyldig, og ikke benyttes senere",
        security = @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_CONFIGURATION_NAME)
    )
    @Parameters(
        {
            @Parameter(ref = "#/components/parameters/" + MdcConfig.HEADER_CALL_ID_KEY),
            @Parameter(ref = "#/components/parameters/" + MdcConfig.HEADER_X_REQUEST_ID_KEY),
            @Parameter(ref = "#/components/parameters/" + MdcConfig.HEADER_CONSUMER_ID_KEY)
        }
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Bad request, failure to supply a required parameter"),
            @ApiResponse(
                responseCode = "401",
                description = "Unauthorized, bearer token not supplied or invalid",
                content = @Content, // Empty response
                headers = {
                    @Header(name = "www-authenticate", description = "Error message from bearer/jwt authentication process")
                }
            )
        }
    )
    @PostMapping(value = "/delete")
    public ResponseEntity<String> deleteNavident(
        @Parameter(description = "Navident som skal slettes") @RequestParam String navident,
        @Parameter(description = "Personid som er knyttet til navidenten") @RequestParam String personid,
        @Parameter(description = "IdentType for navidenten") @RequestParam IdentType identType,
        @Parameter(description = "Skriv CONFIRM for å bekrefte sletting!") @RequestParam String confirm
    ) {
        if (!("CONFIRM".equals(confirm) || "CONFIRM_NO_VALIDATION".equals(confirm))) {
            return ResponseEntity.badRequest().body("Må bekrefte sletting med CONFIRM");
        }

        if (!"CONFIRM_NO_VALIDATION".equals(confirm)) {
            if (!navidenter.containsKey(navident)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Navident finnes ikke");
            }

            Navident eksisterende = navidenter.get(navident);

            if (!eksisterende.getGjeldendePersonid().equals(personid)) {
                logger.info(LogConfig.SECURE_LOGS, "Angitt personid er ikke det som er gjeldende. Gjeldende er: {}", eksisterende.getGjeldendePersonid());
                return ResponseEntity.badRequest().body("Kan angitt personid er ikke det som er gjeldende for navidenten.");
            }
        }

        logger.info(LogConfig.SECURE_LOGS, "Sletter Navident/personid: {}/{}", navident, personid);

        eventSourceProducer.produceSlettEvent(navident, identType, personid);

        return ResponseEntity.ok("Navident %s er slettet".formatted(HtmlUtils.htmlEncode(navident)));
    }
}