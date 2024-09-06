package no.nav.nom.apps.api.authorize.redigereressurs.api;

import com.nimbusds.jose.JOSEObjectType;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.MockOAuth2ServerInitializer;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.authorize.redigereressurs.MockBasisTestOrganisering;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@ActiveProfiles("test")
@ConfigurationPropertiesScan
@ContextConfiguration(classes=TestConfig.class, initializers = MockOAuth2ServerInitializer.class)
@WebMvcTest
public class RestTestBase extends MockBasisTestOrganisering {
    @Autowired
    MockOAuth2Server mockOAuth2Server;
    @MockBean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;
    @Mock
    private Authentication auth;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    Unleash unleash;
    public static final String LOGGED_IN_USER_NAVIDENT = "A321321";
    private static final String OAUTH_VALID_AUDIENCE = "test";
    private static final String OAUTH_VALID_ISSUER = "aad";
    private static final String NOM_WRITE_GROUP = "nomWriteGroup";
    private static final String NOM_ADMIN_GROUP = "nomAdminGroup";

    @BeforeEach
    public void setUnleashUseNewAuth() {
        when(unleash.isEnabled(AuthResolver.UNLEASH_REDIGERE_RESSURS_BRUK_NY_AUTH_RESOLVER)).thenReturn(true);
    }

    @BeforeEach
    void mockOAuth2AuthorizedClientManager() {
        OAuth2AuthorizedClient oAuth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);
        when(oAuth2AuthorizedClientManager.authorize(any())).thenReturn(oAuth2AuthorizedClient);
        when(oAuth2AuthorizedClient.getAccessToken()).thenReturn(
            new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "tokenValue",
                Instant.now(),
                Instant.now().plusSeconds(1000L)
            )
        );
    }

    protected String invalidAudienceToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", "B");
        claims.put("azp_name", "nomUiClient");
        return generateToken(claims, "invalid-audience");
    }

    protected String nomWriteUserToken_Leder_B() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", "B");
        claims.put("groups", List.of(NOM_WRITE_GROUP));
        claims.put("azp_name", "nomUiClient");
        return validToken(claims);
    }


    protected String vanligUserToken_Leder_B() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", "B");
        claims.put("azp_name", "nomUiClient");
        return validToken(claims);
    }

    protected String nomWriteUserToken_Leder_C() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", "A");
        claims.put("groups", List.of(NOM_WRITE_GROUP));
        claims.put("azp_name", "nomUiClient");
        return validToken(claims);
    }

    protected String nomWriteUserToken_DelegertLeder_HR() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", "HR");
        claims.put("groups", List.of(NOM_WRITE_GROUP));
        claims.put("azp_name", "nomUiClient");
        return validToken(claims);
    }

    protected String vanligUserToken_DelegertLeder_HR() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", "HR");
        claims.put("azp_name", "nomUiClient");
        return validToken(claims);
    }

    protected String nomAdminUserToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("NAVident", "WHOEVER");
        claims.put("groups", List.of(NOM_ADMIN_GROUP));
        claims.put("azp_name", "nomUiClient");
        return validToken(claims);
    }

    protected String validToken(Map<String, Object> claims) {
        return generateToken(claims, OAUTH_VALID_AUDIENCE);
    }

    protected String generateToken(Map<String, Object>  claims, String audience) {
        return mockOAuth2Server.issueToken(
            OAUTH_VALID_ISSUER,
            "nomUiClient",
            new DefaultOAuth2TokenCallback(
                "aad",
                "subjectId",
                JOSEObjectType.JWT.getType(),
                List.of(audience),
                claims,
                3600
            )
        ).serialize();
    }
}
