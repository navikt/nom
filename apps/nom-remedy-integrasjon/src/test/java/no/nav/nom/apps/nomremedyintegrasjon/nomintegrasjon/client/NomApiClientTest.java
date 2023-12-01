package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client;

import no.nav.nom.apps.nomremedyintegrasjon.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NomApiClientTest extends TestBase {

    @Autowired
    private NomApiClient nomApiClient;

    private final OAuth2AuthorizedClient oAuth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);

    private final OAuth2AccessToken accessToken = new OAuth2AccessToken(
        OAuth2AccessToken.TokenType.BEARER, "tokenValue", Instant.now(), Instant.now().plusSeconds(1000L)
    );

    @BeforeEach
    void mockOauth() {
        when(oAuth2AuthorizedClientManager.authorize(ArgumentMatchers.any())).thenReturn(oAuth2AuthorizedClient);
        when(oAuth2AuthorizedClient.getAccessToken()).thenReturn(accessToken);
    }

    @Test
    @DisplayName("getOrgEnhet should perform request as expected")
    void testGetOrgEnhet() {

        var orgEnhetId = "some-orgenhet";

        String responseBody = readResourceFile("nom-api/orgenhet_response.json");

        stubFor(
            get(anyUrl())
                .willReturn(
                    aResponse()
                        .withBody(responseBody)
                        .withHeader("Content-Type", "application/json")
                )
        );

        assertThat(nomApiClient.getOrgEnhet(orgEnhetId))
            .isNotNull();

        verify(
            getRequestedFor(
                urlEqualTo(
                    String.format(
                        "/app/orgenheter/%s",
                        orgEnhetId
                    )
                )
            ).withHeader("authorization", matching("Bearer tokenValue"))
        );
    }

    @Test
    @DisplayName("getRessursByNavident should perform request as expected")
    void testGetRessursByNavident() {

        var navident = "X123456";

        stubResponse(
            readResourceFile("nom-api/ressurs_response.json")
        );

        assertThat(nomApiClient.getRessursByNavident(navident))
            .isNotNull();

        verify(
            getRequestedFor(
                urlEqualTo(
                    String.format(
                        "/app/ressurser/byNavident/%s",
                        navident
                    )
                )
            ).withHeader("authorization", matching("Bearer tokenValue"))
        );
    }

    @Test
    @DisplayName("getRessursByFid should perform request as expected")
    void testGetRessursByFid() {

        var fid = "ressurs-uuid";

        stubResponse(
            readResourceFile("nom-api/ressurs_response.json")
        );

        assertThat(nomApiClient.getRessursByFid(fid))
            .isNotNull();

        verify(
            getRequestedFor(
                urlEqualTo(
                    String.format(
                        "/app/ressurser/byFid/%s",
                        fid
                    )
                )
            ).withHeader("authorization", matching("Bearer tokenValue"))
        );
    }


    @Test
    @DisplayName("getOrgtilknytning should perform request as expected")
    void testGetOrgtilknytning() {

        var fid = "orgtilknytning-uuid";

        stubResponse(
            readResourceFile("nom-api/orgtilknytning_response.json")
        );

        assertThat(nomApiClient.getOrgtilknytning(fid))
            .isNotNull();

        verify(
            getRequestedFor(
                urlEqualTo(
                    String.format(
                        "/app/orgtilknytninger/%s",
                        fid
                    )
                )
            ).withHeader("authorization", matching("Bearer tokenValue"))
        );
    }

    private void stubResponse(String responseBody) {
        stubFor(
            get(anyUrl())
                .willReturn(
                    aResponse()
                        .withBody(responseBody)
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

    private static String readResourceFile(String resourceFilePath) {
        Resource resource = new ClassPathResource(resourceFilePath);
        try {
            Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8);
            return FileCopyUtils.copyToString(reader);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
