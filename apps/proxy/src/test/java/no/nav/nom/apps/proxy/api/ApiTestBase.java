package no.nav.nom.apps.proxy.api;

import no.nav.nom.apps.dvh.TestBase;
import no.nav.nom.apps.proxy.common.AppProperties;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;

import static org.mockito.Mockito.mock;

@ContextConfiguration(initializers = {MockOAuth2ServerInitializer.class})
public class ApiTestBase extends TestBase {

    @Autowired
    protected WebTestClient webTestClient;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected MockOAuth2Server oAuth2Server;

    @Autowired
    AppProperties appProperties;

    @MockBean
    protected OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    protected final OAuth2AuthorizedClient oAuth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);

    protected final OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "tokenValue",
            Instant.now(),
            Instant.now().plusSeconds(1000L)
    );

}
