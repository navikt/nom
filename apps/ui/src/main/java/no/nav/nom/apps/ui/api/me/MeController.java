package no.nav.nom.apps.ui.api.me;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.contract.nom.ui.me.MeTo;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeController {

    @GetMapping("/api/v1/me")
    public MeTo me() {
        val auth = SecurityContextHolder.getContext().getAuthentication();
        val principal = (DefaultOidcUser) auth.getPrincipal();
        return new MeTo(principal.getFullName());
    }
}
