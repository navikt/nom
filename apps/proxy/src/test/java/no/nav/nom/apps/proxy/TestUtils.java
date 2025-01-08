package no.nav.nom.apps.proxy;

import com.nimbusds.jose.JOSEObjectType;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;

import java.util.List;
import java.util.Map;

public class TestUtils {

    private static final String VALID_AUDIENCE = "proxy-test";
    private static final String VALID_ISSUER = "issuer1";


    public static String getValidToken(MockOAuth2Server oAuth2Server) {
        return getToken(oAuth2Server, VALID_ISSUER, List.of(VALID_AUDIENCE), Map.of());
    }

    public static String getTokenWithInvalidAudience(MockOAuth2Server oAuth2Server) {
        return getToken(oAuth2Server, VALID_ISSUER, List.of("some-audience"), Map.of());
    }

    public static String getTokenWithUnknownIssuer(MockOAuth2Server oAuth2Server) {
        return getToken(oAuth2Server, "different_issuer", List.of(VALID_AUDIENCE), Map.of());
    }

    public static String getExpiredToken(MockOAuth2Server oAuth2Server) {
        return getToken(oAuth2Server, VALID_ISSUER, List.of(VALID_AUDIENCE), Map.of(), -1000L);
    }

    private static String getToken(MockOAuth2Server oAuth2Server, String issuerId, List<String> audiences, Map<String, Object> claims) {
        return getToken(oAuth2Server, issuerId, audiences, claims, 3600L);
    }

    private static String getToken(MockOAuth2Server oAuth2Server, String issuerId, List<String> audiences, Map<String, Object> claims, Long expiry) {
        return oAuth2Server.issueToken(
                issuerId,
                "clientId",
                new DefaultOAuth2TokenCallback(
                        issuerId,
                        "subjectId",
                        JOSEObjectType.JWT.getType(),
                        audiences,
                        claims,
                        expiry
                )
        ).serialize();
    }

}
