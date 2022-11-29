package no.nav.nom.apps.api.rest.sessioninfo;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
public class SessioninfoController {

    private final String nomWriteGroupObjectId;

    @Autowired
    public SessioninfoController(Environment env) {
        this.nomWriteGroupObjectId = env.getRequiredProperty("authentication.groups.nomWrite");
    }

    @GetMapping("/api/v1/sessioninfo")
    public SessionInfoResponse sessioninfo(@AuthenticationPrincipal Jwt principal) {
        var hasWriteGroup = principal.getClaimAsStringList("groups").stream().anyMatch(it -> it.equals(this.nomWriteGroupObjectId));

        return new SessionInfoResponse(
                principal.getClaimAsString("NAVident"),
                principal.getClaimAsString("name"),
                hasWriteGroup ? List.of(UserRoles.ROLE_WRITE) : Collections.emptyList()
        );
    }

    public enum UserRoles {
        ROLE_WRITE,
    }

    public record SessionInfoResponse(
            String navident,
            String name,
            List<UserRoles> roles
    ) {}

}
