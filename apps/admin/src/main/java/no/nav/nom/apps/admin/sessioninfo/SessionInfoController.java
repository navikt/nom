package no.nav.nom.apps.admin.sessioninfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import no.nav.nom.apps.admin.SwaggerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
public class SessionInfoController {
    
    private final Logger logger = LoggerFactory.getLogger(SessionInfoController.class);
    
    private final Map<String, UserRoles> claimToRoleMapping;

    public SessionInfoController(Environment env) {
        this.claimToRoleMapping = Map.of(
             env.getRequiredProperty("authentication.groups.nomAdmin"), UserRoles.ROLE_WRITE,
             env.getRequiredProperty("authentication.groups.supportReadOnly"), UserRoles.ROLE_READONLY
        );
        logger.info("Roles mapping: {}", claimToRoleMapping);
    }

    @Operation(
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/sessioninfo")
    public SessionInfoResponse sessionInfo(@AuthenticationPrincipal Jwt principal) {
        logger.info("SessionInfo groups for {}: {}", principal.getClaimAsString("NAVident"), principal.getClaimAsString("groups"));
        
        List<UserRoles> userRoles = principal.getClaimAsStringList("groups").stream()
                .map(group -> claimToRoleMapping.getOrDefault(group, null))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        
        return new SessionInfoResponse(
                principal.getClaimAsString("NAVident"),
                principal.getClaimAsString("name"),
                userRoles
        );
    }

    @Schema(enumAsRef = true)
    public enum UserRoles {
        ROLE_WRITE,
        ROLE_READONLY
    }

    public record SessionInfoResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<UserRoles> roles
    ) {}
    
}
