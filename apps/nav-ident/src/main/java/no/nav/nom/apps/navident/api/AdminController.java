package no.nav.nom.apps.navident.api;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.navident.common.SwaggerConfig;
import no.nav.nom.apps.navident.job.ImportNavIdenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Navident")
@RestController
@RequestMapping(AdminController.BASE_URL)
public class AdminController {

    public final static String BASE_URL = "/admin";
    private final static Logger logger = LoggerFactory.getLogger(AdminController.class);
    public static final String NAV_IDENT_RESURS_FILE = "/NIK_identType.txt";
    private final ImportNavIdenter importNavIdenter;
    private final Unleash unleash;
    public static final String UNLEASH_PRODUSERE_ENDRE_NAV_IDENT_MELDINGER = "nom.nav_ident.enable_edit_topics";

    public AdminController(ImportNavIdenter importNavIdenter, Unleash unleash) {
        this.importNavIdenter = importNavIdenter;
        this.unleash = unleash;
    }

    @Operation(
            description = "Importer NavIdenter. One-off job for å importere navidenter og personidenter fra Remedy",
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
    @PreAuthorize("hasRole('ROLE_NOM_WRITE')")
    public String importerNavIdenter(@RequestParam String bucketObjectName, @RequestParam(required = false) boolean kunLes) {
        logger.info("Starter NavIdent innlesning. kunLes={}", kunLes);
        return importNavIdenter.fromBucketObject(bucketObjectName, kunLes);
    }

    @GetMapping("/importerNavIdenterFraResursFil")
    @PreAuthorize("hasRole('ROLE_NOM_WRITE')")
    public String importIdentTypeFromResourceFile() {
        logger.info("Starter NavIdent innlesning fra resursfil");
        if (unleash.isEnabled(UNLEASH_PRODUSERE_ENDRE_NAV_IDENT_MELDINGER)){
            return importNavIdenter.importIdentTypeFromResourceFile(getClass().getResourceAsStream(NAV_IDENT_RESURS_FILE));
        }
        else {
            return "Feature flag " + UNLEASH_PRODUSERE_ENDRE_NAV_IDENT_MELDINGER + " er ikke aktivert";
        }
    }


}