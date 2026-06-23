package no.nav.nom.apps.api.rest.sessioninfo;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@Slf4j
public class SessionInfoController {
    private final Map<String, UserRoles> claimToRoleMap;

    @Autowired
    public SessionInfoController(Environment env) {
        this.claimToRoleMap = new HashMap<>();
        claimToRoleMap.put(env.getRequiredProperty("authentication.groups.nomDelegert"), UserRoles.ROLE_WRITE);
        claimToRoleMap.put(env.getRequiredProperty("authentication.groups.nomLeder"), UserRoles.ROLE_WRITE);
        claimToRoleMap.put(env.getRequiredProperty("authentication.groups.nomWrite"), UserRoles.ROLE_WRITE);
        claimToRoleMap.put(env.getRequiredProperty("authentication.groups.nomAdmin"), UserRoles.ROLE_ADMIN);
        claimToRoleMap.put(env.getRequiredProperty("authentication.groups.nomPilot"), UserRoles.ROLE_PILOT);
        claimToRoleMap.put(env.getRequiredProperty("authentication.groups.nomWriteUtlKonsulent"), UserRoles.ROLE_WRITE_UTL_KONSULENT);
        claimToRoleMap.put(env.getRequiredProperty("authentication.groups.nomWriteVikafossen"), UserRoles.ROLE_WRITE_VIKAFOSSEN);
        claimToRoleMap.put(env.getRequiredProperty("authentication.groups.nomWriteRobot"), UserRoles.ROLE_WRITE_ROBOT);
        claimToRoleMap.put(env.getRequiredProperty("authentication.groups.readOnly"), UserRoles.ROLE_READONLY);
        claimToRoleMap.put(env.getRequiredProperty("authentication.groups.extendedReadOnly"), UserRoles.ROLE_EXTENDED_READONLY);
        claimToRoleMap.put(env.getRequiredProperty("authentication.groups.kostnadsstedsAdmin"), UserRoles.ROLE_KOSTNADSTED_MAPPING_ADMIN);
        claimToRoleMap.put(env.getRequiredProperty("authentication.groups.nomWriteExternalFundsRecipient"), UserRoles.ROLE_EXTERNAL_FUNDS_RECIPIENT);
        log.debug("authentication.groups Environment (claimToRoleMap): {}", claimToRoleMap);
    }

    @Operation(
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping(value={"/app/sessioninfo"})
    public SessionInfoResponse sessioninfo(@AuthenticationPrincipal Jwt principal) {
        log.debug("Sessioninfo groups for {}: {}", principal.getClaimAsString("NAVident"), principal.getClaimAsStringList("groups"));
        var userRoles = principal.getClaimAsStringList("groups").stream()
            .map(group -> claimToRoleMap.getOrDefault(group, null))
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
        ROLE_ADMIN,
        ROLE_PILOT,
        ROLE_WRITE_UTL_KONSULENT,
        ROLE_WRITE_VIKAFOSSEN,
        ROLE_WRITE_ROBOT,
        ROLE_READONLY,
        ROLE_EXTENDED_READONLY,
        ROLE_EXTERNAL_FUNDS_RECIPIENT,
        ROLE_KOSTNADSTED_MAPPING_ADMIN
    }

    public record SessionInfoResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String navident,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<UserRoles> roles
    ) {}

}
