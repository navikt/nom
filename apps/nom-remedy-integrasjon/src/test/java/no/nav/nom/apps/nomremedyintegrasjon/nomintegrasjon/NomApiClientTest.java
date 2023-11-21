package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import no.nav.nom.apps.nomremedyintegrasjon.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
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
    void mockOauth(){
        when(oAuth2AuthorizedClientManager.authorize(ArgumentMatchers.any())).thenReturn(oAuth2AuthorizedClient);
        when(oAuth2AuthorizedClient.getAccessToken()).thenReturn(accessToken);
    }

    @Test
    @DisplayName("getOrgEnhet should perform requests as expected")
    void testGetOrgEnhet(){

        var orgEnhetId = "some-orgenhet";
        var remedyId = "some-remedy-id";

        String responseBody = String.format("""
            {
              "data": {
                "orgEnhet": {
                  "id": "%s",
                  "remedyEnhetId": "%s"
                }
              }
            }
            """, orgEnhetId, remedyId);


        stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(
                    aResponse()
                        .withBody(responseBody)
                        .withHeader("Content-Type", "application/json")
                )
        );

        assertThat(nomApiClient.getOrgEnhetWithRemedyId(orgEnhetId))
            .isEqualTo(Optional.of(new OrgEnhetWithRemedyId(orgEnhetId, remedyId)));


        String expectedRequestBody = String.format(
            """
            {
              "query": "query getOrgEnhet($id: String!) {\\n    orgEnhet(where: {id: $id}) {\\n        id\\n        remedyEnhetId\\n    }\\n}",
              "variables": {
                "id": "%s"
              },
              "operationName": "getOrgEnhet"
            }
            """,
            orgEnhetId
        );

        verify(
            postRequestedFor(urlEqualTo("/graphql"))
                .withRequestBody(equalToJson(expectedRequestBody))
                .withHeader("authorization", matching("Bearer tokenValue"))
        );

    }
}
