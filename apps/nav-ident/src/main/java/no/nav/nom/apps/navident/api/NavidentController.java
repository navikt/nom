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
import no.nav.nom.apps.navident.api.dto.NavidentLookupRequestDto;
import no.nav.nom.apps.navident.api.dto.NavidentLookupResponseDTO;
import no.nav.nom.apps.navident.api.dto.NavidentRequestDTO;
import no.nav.nom.apps.navident.api.dto.NavidentResponseDTO;
import no.nav.nom.apps.navident.common.LogConfig;
import no.nav.nom.apps.navident.common.SwaggerConfig;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.libs.utils.common.mdc.MdcConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.Normalizer;
import java.util.Map;


@Tag(name = "Navident")
@RestController
@RequestMapping(NavidentController.BASE_URL)
public class NavidentController {
    private final Map<String, Navident> navidenter;
    public final static String BASE_URL = "/navident";
    private final static Logger logger = LoggerFactory.getLogger(NavidentController.class);
    private final IdentService identService;

    public NavidentController(Map<String, Navident> navidenter, IdentService identService) {
        this.navidenter = navidenter;
        this.identService = identService;
    }

    @Operation(
        description = "Bestill en nav-ident - e.g lager ny navident",
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
        logger.info(LogConfig.TEAM_LOGS, "Navident bestilt for: {}", request);
        val bestilling = identService.bestillIdent(request.getPersonid(), request.getIdentType(), prefixValidator(request.getIdentPrefix()));
        val response = NavidentResponseDTO.builder()
            .navident(bestilling.getBody().getNavident())
            .feilmelding(bestilling.getBody().getFeilmelding())
            .build();

        return ResponseEntity.status(bestilling.getHttpStatus()).body(response);
    }

    @PostMapping(value = "/lookup")
    public ResponseEntity<NavidentLookupResponseDTO> lookupNavident(@RequestBody NavidentLookupRequestDto request) {
        logger.info(LogConfig.TEAM_LOGS, "Lookup navident for: {}", request);
        var maybePersonIdentMatch = navidenter.values()
            .stream()
            .filter(navidentEntry -> navidentEntry.getGjeldendePersonid().equals(request.getPersonid()))
            .findFirst();

        return maybePersonIdentMatch.map(personIdentMatch -> ResponseEntity.ok(
            NavidentLookupResponseDTO.builder()
                    .identType(personIdentMatch.getIdentType())
                    .navident(personIdentMatch.getNavident())
                .build()
            )).orElse(ResponseEntity.notFound().build());
    }

    char prefixValidator(String identPrefix) {
        char prefixChar = Normalizer.normalize(identPrefix.toUpperCase(), Normalizer.Form.NFKD).charAt(0);
        switch (prefixChar) {
            case 'Æ':
                return 'A';
            case 'Ø':
                return 'O';
            case 'Å':
                return 'A';
        }
        if ( prefixChar < 'A' || prefixChar > 'Z' ) {
            return 'W';
        }
        return prefixChar;
    }
}