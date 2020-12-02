package no.nav.nom.apps.ui.common.security;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static no.nav.nom.apps.ui.common.security.CookieUtils.ACCESS_TOKEN_COOKIE_KEY;
import static no.nav.nom.apps.ui.common.security.CookieUtils.ID_TOKEN_COOKIE_KEY;
import static no.nav.nom.apps.ui.common.security.CookieUtils.REFRESH_TOKEN_COOKIE_KEY;
import static no.nav.nom.apps.ui.common.security.CookieUtils.SEVEN_DAYS;

@Component
@RequiredArgsConstructor
class CookieBasedAuthorizedClientRepository implements OAuth2AuthorizedClientRepository {

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request) {
        return null;
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
        val idToken = ((DefaultOidcUser) ((OAuth2AuthenticationToken) principal).getPrincipal()).getIdToken().getTokenValue();
        val accessToken = authorizedClient.getAccessToken().getTokenValue();
        val refreshToken = authorizedClient.getRefreshToken().getTokenValue();

        response.addCookie(CookieUtils.createCookie(ID_TOKEN_COOKIE_KEY, idToken, true, SEVEN_DAYS));
        response.addCookie(CookieUtils.createCookie(ACCESS_TOKEN_COOKIE_KEY, accessToken, true, SEVEN_DAYS));
        response.addCookie(CookieUtils.createCookie(REFRESH_TOKEN_COOKIE_KEY, refreshToken, true, SEVEN_DAYS));
    }

    @Override
    public void removeAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
        response.addCookie(CookieUtils.createClearingCookie(ID_TOKEN_COOKIE_KEY, true));
        response.addCookie(CookieUtils.createClearingCookie(ACCESS_TOKEN_COOKIE_KEY, true));
        response.addCookie(CookieUtils.createClearingCookie(REFRESH_TOKEN_COOKIE_KEY, true));
    }
}
