package no.nav.nom.apps.ui.me;

import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.RefreshTokenParameters;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.ui.common.security.CookieUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static no.nav.nom.apps.ui.azure.AadOAuth2AccessTokenResponseClient.MICROSOFT_GRAPH_SCOPES;
import static no.nav.nom.apps.ui.common.security.CookieUtils.ACCESS_TOKEN_COOKIE_KEY;
import static no.nav.nom.apps.ui.common.security.CookieUtils.ID_TOKEN_COOKIE_KEY;
import static no.nav.nom.apps.ui.common.security.CookieUtils.SEVEN_DAYS;
import static no.nav.nom.apps.ui.common.security.SecurityConfig.ROTATION_ENDPOINT;

@RestController
@RequiredArgsConstructor
public class MeController {
    private final ConfidentialClientApplication msalClient;

    @GetMapping("/api/v1/me")
    public Object me() {
        return null;
    }

    @GetMapping(ROTATION_ENDPOINT)
    public ResponseEntity<?> rotate(HttpServletRequest request, HttpServletResponse response) {
        val refreshToken = CookieUtils.extractCookieValue(CookieUtils.REFRESH_TOKEN_COOKIE_KEY, request.getCookies());
        if (refreshToken == null) {
            ResponseEntity.badRequest().body("Failed to find Refresh Token");
        } else {
            try {
                val result = msalClient.acquireToken(RefreshTokenParameters.builder(MICROSOFT_GRAPH_SCOPES, refreshToken).build()).get();

                val idToken = result.idToken();
                val accessToken = result.accessToken();

                response.addCookie(CookieUtils.createCookie(ID_TOKEN_COOKIE_KEY, idToken, true, SEVEN_DAYS));
                response.addCookie(CookieUtils.createCookie(ACCESS_TOKEN_COOKIE_KEY, accessToken, true, SEVEN_DAYS));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unknown"); // Litt usikker på hvilke feil vi kan få.
            }
        }
        return null;
    }
}
