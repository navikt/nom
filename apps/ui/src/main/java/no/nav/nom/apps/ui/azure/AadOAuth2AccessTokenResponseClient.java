package no.nav.nom.apps.ui.azure;

import com.microsoft.aad.msal4j.AuthorizationCodeParameters;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IConfidentialClientApplication;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import static org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames.ID_TOKEN;

/**
 * Azure AD returnerer ikke refreshToken på "vanlige" kall, det må komme via deres MSAL client.
 * Dette er da lagd for å støtte denne funksjonaliteten.
 */
@Slf4j
@RequiredArgsConstructor
public class AadOAuth2AccessTokenResponseClient implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {
    public static final Set<String> MICROSOFT_GRAPH_SCOPES = Set.of("openid", "https://graph.microsoft.com/user.read");

    private final IConfidentialClientApplication msalClient;
    private final MeterRegistry meterRegistry;

    @Override
    @SneakyThrows
    @Timed("azureaad.getToken")
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest request) {
        try {
            val result = msalClient.acquireToken(AuthorizationCodeParameters
                    .builder(request.getAuthorizationExchange().getAuthorizationResponse().getCode(), new URI(request.getAuthorizationExchange().getAuthorizationResponse().getRedirectUri()))
                    .scopes(MICROSOFT_GRAPH_SCOPES)
                    .build())
                    .get();

            val refreshToken = getRefreshTokenFromAuthResult(result);

            meterRegistry.counter("azureaad.aquireToken", "status", "ok").increment();

            return OAuth2AccessTokenResponse.withToken(result.accessToken())
                    .refreshToken(refreshToken)
                    .scopes(Set.of(result.scopes().split(" ")))
                    .tokenType(OAuth2AccessToken.TokenType.BEARER)
                    .additionalParameters(Map.of(ID_TOKEN, result.idToken()))
                    .build();
        } catch (Exception e) {
            log.error("Failed to get tokens from AzureAD, Message: {}", e.getMessage());
            meterRegistry.counter("azureaad.aquireToken", "status", "failed").increment();
            throw e;
        }
    }

    @SneakyThrows // GG Microsoft
    private String getRefreshTokenFromAuthResult(IAuthenticationResult authResult) {
        val refreshTokenMethod = ReflectionUtils.findMethod(Class.forName("com.microsoft.aad.msal4j.AuthenticationResult"), "refreshToken");
        Assert.notNull(refreshTokenMethod, "couldn't find refreshToken method");
        refreshTokenMethod.setAccessible(true);
        return (String) refreshTokenMethod.invoke(authResult);
    }
}
