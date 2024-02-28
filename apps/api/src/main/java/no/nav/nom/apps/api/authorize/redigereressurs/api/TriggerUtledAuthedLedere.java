package no.nav.nom.apps.api.authorize.redigereressurs.api;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.common.SwaggerConfig;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin for å utlede authorized ledere på nytt.")
@RestController
@Slf4j
@RequestMapping("/api/admin/utledauthedledererforredigereressurs")
@PreAuthorize("hasAuthority('ADMIN')")
public class TriggerUtledAuthedLedere {

    private final AuthorizedNavident authorizedNavident;

    public TriggerUtledAuthedLedere(AuthorizedNavident authorizedNavident) {
        this.authorizedNavident = authorizedNavident;
    }

    @Operation(
        description = "Utled ledere for orgenheter som har tilgang til å redigere ressurser.",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping()
    void utledAuthedLedere() {
        authorizedNavident.oppdaterLedereForOrgenheter();
    }
}
