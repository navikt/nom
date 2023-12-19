package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static no.nav.nom.apps.api.rest.admin.RessursSammenstillingAvInformasjon.RESSURS_SAMMENSTILLING;

@Slf4j
@Tag(name = "Admin for datagrunnlag og sammenstilling")
@RestController
@RequestMapping("/api/admin")
public class RessursSammenstillingAvInformasjonController {
    private final RessursSammenstillingAvInformasjon ressursSammenstillingAvInformasjon;
    private final LeaderElection leaderElection;
    private final AsyncAdminRunStatus asyncAdminRunStatus;


    public RessursSammenstillingAvInformasjonController(RessursSammenstillingAvInformasjon ressursSammenstillingAvInformasjon, LeaderElection leaderElection, AsyncAdminRunStatus asyncAdminRunStatus) {
        this.ressursSammenstillingAvInformasjon = ressursSammenstillingAvInformasjon;
        this.leaderElection = leaderElection;
        this.asyncAdminRunStatus = asyncAdminRunStatus;
    }

    @Operation(
        description = "Sammenstiller informasjon fra NOM og DVH (U4/Remedy)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/RessursSammenstillingAvInformasjon")
    public String lagSammenstilling(
        @Parameter(description = "Begrense informasjonen til Sektor (NAV_STATLIG, NAV_KOMMUNAL, EKSTERN). For alle, la stå tom") @RequestParam(required = false) String sektor,
        @Parameter(description = "Begrense til angitte navidenter - kommaseparert. Las stå tom for alle") @RequestParam(required = false) String navidenter,
        @Parameter(description = "Kun aktive? default false") @RequestParam(defaultValue = "true") boolean bareAktive,
        @NonNull JwtAuthenticationToken token
    ) {
        if (!leaderElection.isLeader()) {
            return "Sry - traff pod som ikke er leader. Kan ikke starte kjøring";
        }
        if (!asyncAdminRunStatus.isEnded(RESSURS_SAMMENSTILLING)) {
            return "Sry - kjøring allerede startet";
        }
        ressursSammenstillingAvInformasjon.runSammenstilling(sektor, navidenter, bareAktive);
        return asyncAdminRunStatus.startNewRun(RESSURS_SAMMENSTILLING);
    }

    @Operation(
        description = "Få status på siste kjøring av Sammenstilling av informasjon fra NOM og DVH (U4/Remedy)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success")
    })
    @GetMapping(value = "/RessursSammenstillingAvInformasjonStatus")
    public String sammenstillingKjoringStatus() {
        return asyncAdminRunStatus.report(RESSURS_SAMMENSTILLING, null);
    }

    @Operation(
        description = "Få resultatet på siste kjøring av Sammenstilling av informasjon fra NOM og DVH (U4/Remedy)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success")
    })
    @GetMapping(value = "/RessursSammenstillingAvInformasjonResultat")
    public List<RessursSammenstillingAvInformasjon.RessursSammenstilling> sammenstillingKjoringResultat() {
        if (!leaderElection.isLeader()) {
            return null;
        }
        if (asyncAdminRunStatus.isEnded(RESSURS_SAMMENSTILLING)) {
            return ressursSammenstillingAvInformasjon.runResult();
        }
        return null;
    }
}

