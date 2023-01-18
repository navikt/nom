package no.nav.nom.apps.navident.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.navident.common.SwaggerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Navident")
@RestController
@RequestMapping(AdminController.BASE_URL)
public class AdminController {

    public final static String BASE_URL = "/admin";
    private final static Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Operation(
            description = "Test",
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
    @GetMapping
    @PreAuthorize("hasRole('ROLE_${group.id.nom.write}')")
    public String test() {
        logger.info("Test admin url invoked");
        return "Autorisert";
    }
}