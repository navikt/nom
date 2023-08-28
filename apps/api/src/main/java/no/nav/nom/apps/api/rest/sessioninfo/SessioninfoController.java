package no.nav.nom.apps.api.rest.sessioninfo;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import no.nav.nom.apps.api.common.SwaggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
public class SessioninfoController {
    private final Map<String, UserRoles> claimToRoleMap;

    @Autowired
    public SessioninfoController(Environment env) {

        this.claimToRoleMap = Map.of(
            env.getRequiredProperty("authentication.groups.nomWrite"), UserRoles.ROLE_WRITE,
            env.getRequiredProperty("authentication.groups.nomAdmin"), UserRoles.ROLE_ADMIN
        );
    }

    @Operation(
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/api/v1/sessioninfo")
    public SessionInfoResponse sessioninfo(@AuthenticationPrincipal Jwt principal) {
        var userRoles = principal.getClaimAsStringList("groups").stream()
            .map(group -> claimToRoleMap.getOrDefault(group, null))
            .filter(Objects::nonNull)
            .toList();

        return new SessionInfoResponse(
                principal.getClaimAsString("NAVident"),
                principal.getClaimAsString("name"),
                userRoles
        );
    }

    public enum UserRoles {
        ROLE_WRITE,
        ROLE_ADMIN
    }

    public record SessionInfoResponse(
            String navident,
            String name,
            List<UserRoles> roles
    ) {}

}
