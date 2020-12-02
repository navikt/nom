package no.nav.nom.apps.ui.common.security;

import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import static no.nav.nom.apps.ui.common.security.CookieUtils.ID_TOKEN_COOKIE_KEY;

@Component
class CookieBasedTokenResolver implements BearerTokenResolver {
    @Override
    public String resolve(HttpServletRequest request) {
        return CookieUtils.extractCookieValue(ID_TOKEN_COOKIE_KEY, request.getCookies());
    }
}
