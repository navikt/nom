package no.nav.nom.apps.navident.api;

import io.getunleash.Unleash;
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
import no.nav.nom.apps.navident.api.dto.NavidentRequestDTO;
import no.nav.nom.apps.navident.api.dto.NavidentResponseDTO;
import no.nav.nom.apps.navident.api.dto.RegisterNavidentAndPersonidentRequestDTO;
import no.nav.nom.apps.navident.common.LogConfig;
import no.nav.nom.apps.navident.common.SwaggerConfig;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.eventsource.EventSourceProducer;
import no.nav.nom.apps.navident.job.ImportNavIdenter;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.libs.utils.common.mdc.MdcConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;
import java.util.Objects;


@Tag(name = "Navident")
@RestController
@RequestMapping(AdminController.BASE_URL)
public class AdminController {

    public final static String BASE_URL = "/admin";
    private final static Logger logger = LoggerFactory.getLogger(AdminController.class);
    public static final String NAV_IDENT_RESURS_FILE = "/NIK_identType.txt";
    private final ImportNavIdenter importNavIdenter;
    private final Map<String, Navident> navidenter;
    private final EventSourceProducer eventSourceProducer;

    public AdminController(ImportNavIdenter importNavIdenter, Map<String, Navident> navidenter, EventSourceProducer eventSourceProducer) {
        this.importNavIdenter = importNavIdenter;
        this.navidenter = navidenter;
        this.eventSourceProducer = eventSourceProducer;
    }

    @Operation(
            description = "IKKE Kjør denne i Swagger - Swagger poster automatisk retry når det tar tid uten at respons kommer.\n " +
                "Bruk heller curl:\n" +
                "curl -X 'GET' 'https://nav-ident.intern.nav.no/admin/importerNavIdenter?bucketObjectName=Remedy_eksport_fra_produksjon_utf8.csv&kunLes=true' " +
                "  -H 'accept: */*' -H 'Authorization: Bearer $TOKEN'\n" +
                "Importer NavIdenter. One-off job for å importere navidenter og personidenter fra Remedy",
            security = @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_CONFIGURATION_NAME)
    )
    @ApiResponses(
            {
                    @ApiResponse(responseCode = "200", description = "Success"),
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
    @GetMapping("/importerNavIdenter")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String importerNavIdenter(@RequestParam String bucketObjectName, @RequestParam(required = false) boolean kunLes) {
        logger.info("Starter NavIdent innlesning. kunLes={}", kunLes);
        return importNavIdenter.fromBucketObject(bucketObjectName, kunLes);
    }

    @GetMapping("/importerNavIdenterFraResursFil")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String importIdentTypeFromResourceFile() {
        logger.info("Starter NavIdent innlesning fra resursfil");
        return importNavIdenter.importIdentTypeFromResourceFile(getClass().getResourceAsStream(NAV_IDENT_RESURS_FILE));
    }

    @Operation(
        description = "Opprett knytning av navident og personident - lager ingen ny navident, bruker angitt navident",
        security = @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_CONFIGURATION_NAME)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
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
    @PostMapping(value = "/registerNavidentAndPersonident")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity <String> linkNavident(
        @RequestBody RegisterNavidentAndPersonidentRequestDTO request,
        @Parameter(description = "Skriv CONFIRM for å bekrefte registrering av Navident/personident") @RequestParam String confirm)
    {
        logger.info(LogConfig.TEAM_LOGS, "Navident/personident registreres for: {}", request);

        if (navidenter.containsKey(request.getNavident())) {
            return ResponseEntity.badRequest().body("Navident finnes allerede");
        }

        if (navidenter.values().stream().anyMatch(n -> Objects.equals(n.getGjeldendePersonid(), request.getPersonid()))) {
            return ResponseEntity.badRequest().body("Personid finnes allerede");
        }


        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.ok("Registrering av Navident/personident må bekreftes med CONFIRM - parameterene er validert ok");
        }

        eventSourceProducer.produceOpprettNavidentEvent(
            request.getNavident(),
            request.getIdentType(),
            request.getPersonid()
        );

        return ResponseEntity.ok("Registrering av Navident/personident er fullført");
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
    @PreAuthorize("hasRole('ROLE_ADMIN')")
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
                logger.info(LogConfig.TEAM_LOGS, "Angitt personid er ikke det som er gjeldende. Gjeldende er: {}", eksisterende.getGjeldendePersonid());
                return ResponseEntity.badRequest().body("Kan angitt personid er ikke det som er gjeldende for navidenten.");
            }
        }

        logger.info(LogConfig.TEAM_LOGS, "Sletter Navident/personid: {}/{}", navident, personid);

        eventSourceProducer.produceSlettEvent(navident, identType, personid);

        return ResponseEntity.ok("Navident %s er slettet".formatted(HtmlUtils.htmlEscape(navident)));
    }
}