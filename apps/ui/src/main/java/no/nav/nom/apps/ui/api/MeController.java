package no.nav.nom.apps.ui.api;

import lombok.RequiredArgsConstructor;
import no.nav.nom.contract.nom.ui.me.MeTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequiredArgsConstructor
class MeController extends AbstractAuthenticatedApi {

    @GetMapping("/api/v1/me")
    MeTo me(@AuthenticationPrincipal DefaultOidcUser principal, HttpServletRequest req, HttpServletResponse res) {
        rotateAccessKeyIfExpired(req, res);

        return new MeTo(principal.getFullName());
    }
}
