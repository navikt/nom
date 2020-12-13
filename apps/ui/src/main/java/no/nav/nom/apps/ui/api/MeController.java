package no.nav.nom.apps.ui.api;

import lombok.RequiredArgsConstructor;
import no.nav.nom.contract.nom.ui.me.MeTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class MeController {

    @GetMapping("/api/v1/me")
    MeTo me(@AuthenticationPrincipal DefaultOidcUser principal) {
        return new MeTo(principal.getFullName());
    }
}
