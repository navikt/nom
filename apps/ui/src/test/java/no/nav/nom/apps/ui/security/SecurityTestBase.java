package no.nav.nom.apps.ui.security;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.val;
import no.nav.nom.apps.ui.TestBase;
import no.nav.nom.apps.ui.azure.AadOAuth2AccessTokenResponseClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames.ID_TOKEN;

public class SecurityTestBase extends TestBase {

    @Autowired
    protected AadOAuth2AccessTokenResponseClient tokenResponseClientMock;

    @Autowired
    private RSAKey rsaKey;

    protected void mockTokenResponseClientWith(JWTClaimsSet.Builder accessToken, JWTClaimsSet.Builder idToken, String refreshToken) {
        val response = OAuth2AccessTokenResponse.withToken(securityUtils.createToken(accessToken))
                .refreshToken(refreshToken)
                .scopes(Set.of("derp"))
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .additionalParameters(Map.of(ID_TOKEN, securityUtils.createToken(idToken)))
                .build();

        when(tokenResponseClientMock.getTokenResponse(any())).thenReturn(response);
    }

    protected JWTClaimsSet.Builder validIdToken(String nonce) {
        return securityUtils.validJwtClaims().claim("nonce", nonce);
    }

    protected JWTClaimsSet.Builder validAccessToken() {
        return securityUtils.validJwtClaims();
    }

    protected String refreshToken() {
        return UUID.randomUUID().toString(); // Inneholder ikke data på lik linje som idToken og accessToken.
    }

}
