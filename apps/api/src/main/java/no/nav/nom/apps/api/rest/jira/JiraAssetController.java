package no.nav.nom.apps.api.rest.jira;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.common.SwaggerConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Jira Asset")
@RestController
@RequestMapping("/api/v1/jira")
@RequiredArgsConstructor
public class JiraAssetController {

    private final JiraAssetRestService jiraAssetRestService;

    @Operation(
        description = "Hente ut alle fysiskeBrukersteder og antall ansatte",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/fysiskeBrukersteder")
    public ResponseEntity<List<JiraAssetResponse>> fysiskeBrukerstederMedAlleAnsatte() {
        return ResponseEntity.status(HttpStatus.OK).body(jiraAssetRestService.getFysiskBrukerStedWithRessurser());
    }
}
