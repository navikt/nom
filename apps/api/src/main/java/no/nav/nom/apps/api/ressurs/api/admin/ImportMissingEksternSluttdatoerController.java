package no.nav.nom.apps.api.ressurs.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.ressurs.job.MissingEksternSluttdatoer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ImportMissingEksternSluttdatoerController.BASE_URL)
@PreAuthorize("hasAuthority('ADMIN')")
public class ImportMissingEksternSluttdatoerController {

    private final MissingEksternSluttdatoer missingEksternSluttdatoer;

    public static final String BASE_URL = "/api/admin";

    public ImportMissingEksternSluttdatoerController(MissingEksternSluttdatoer missingEksternSluttdatoer) {
        this.missingEksternSluttdatoer = missingEksternSluttdatoer;
    }

    @Operation(
        description = "Importerer sluttdatoer for eksterne fra csv fil i google bucket",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/importMissingEksternSluttdatoer")
    public String importMissing(
        @RequestParam String fileName,
        @RequestParam(required = false, defaultValue = "true") boolean dryRun) {
        return missingEksternSluttdatoer.importer(fileName, dryRun);
    }
}
