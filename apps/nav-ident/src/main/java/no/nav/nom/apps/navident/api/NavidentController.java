package no.nav.nom.apps.navident.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.val;
import no.nav.nom.apps.navident.IdentService;
import no.nav.nom.apps.navident.api.dto.NavidentRequestDTO;
import no.nav.nom.apps.navident.api.dto.NavidentResponseDTO;
import no.nav.nom.apps.navident.common.LogConfig;
import no.nav.nom.libs.utils.common.mdc.MdcConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Navident")
@RestController
@RequestMapping(NavidentController.BASE_URL)
public class NavidentController {

    public final static String BASE_URL = "/navident";
    private final static Logger logger = LoggerFactory.getLogger(NavidentController.class);

    private final IdentService identService;

    public NavidentController(IdentService identService) {
        this.identService = identService;
    }

    @Operation(
            description = "Bestill en nav-ident",
            parameters = {
                    @Parameter(
                            name = MdcConfig.HEADER_CALL_ID_KEY,
                            description = "Call/Correlation id for this request",
                            example = "${random.uuid}",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string", format = "uuid")
                    ),
                    @Parameter(
                            name = MdcConfig.HEADER_X_REQUEST_ID_KEY,
                            description = "Alternate call/correlation id, if missing, will be the same value as " + MdcConfig.HEADER_CALL_ID_KEY,
                            example = "${random.uuid}",
                            in = ParameterIn.HEADER,
                            schema = @Schema(type = "string")
                    ),
                    @Parameter(
                            name = MdcConfig.HEADER_CONSUMER_ID_KEY,
                            description = "Calling application identifier",
                            example = "${spring.application.name}",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")
                    )
            }
    )
    @PostMapping
    public ResponseEntity<NavidentResponseDTO> createNavident(@Parameter(name = "NavidentRequest") @RequestBody NavidentRequestDTO request) {
        logger.info(LogConfig.SECURE_LOGS, "Navident bestilt for: {}", request);
        val bestilling = identService.bestillIdent(request.getPersonid(), request.getIdentType(), request.getIdentPrefix().charAt(0));
        val response = NavidentResponseDTO.builder()
                .navident(bestilling.getBody().getNavident())
                .feilmelding(bestilling.getBody().getFeilmelding())
                .build();

        return ResponseEntity.status(bestilling.getHttpStatus()).body(response);
    }
}