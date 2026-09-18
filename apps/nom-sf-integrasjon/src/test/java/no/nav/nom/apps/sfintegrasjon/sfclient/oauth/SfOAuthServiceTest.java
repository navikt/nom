package no.nav.nom.apps.sfintegrasjon.sfclient.oauth;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import no.nav.nom.apps.sfintegrasjon.sfclient.SfProperties;
import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SamlAssertionParams;
import no.nav.nom.apps.sfintegrasjon.sfclient.saml.SamlAssertionService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WireMockTest
class SfOAuthServiceTest {

    private static final String MOCK_ASSERTION = "bW9ja0Jhc2U2NEFzc2VydGlvbg==";

    @Test
    void shouldPostCorrectFormParametersAndReturnToken(WireMockRuntimeInfo wm) {
        stubFor(post("/oauth/token")
            .willReturn(okJson("""
                {"access_token":"sf-access-token","token_type":"Bearer","expires_in":86400}
                """)));

        SfOAuthService service = buildService(wm.getHttpPort());
        SfAccessToken token = service.generateToken(SamlAssertionParams.defaults());

        assertThat(token.accessToken()).isEqualTo("sf-access-token");
        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.expiresIn()).isEqualTo(86400);

        verify(postRequestedFor(urlEqualTo("/oauth/token"))
            .withRequestBody(containing("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Asaml2-bearer"))
            .withRequestBody(containing("client_id=test-client-id"))
            .withRequestBody(containing("company_id=test-tenant"))
            .withRequestBody(containing("assertion=" + URLEncoder.encode(MOCK_ASSERTION, StandardCharsets.UTF_8))));
    }

    @Test
    void shouldUseParamOverridesInsteadOfDefaults(WireMockRuntimeInfo wm) {
        stubFor(post("/oauth/token")
            .willReturn(okJson("""
                {"access_token":"override-token","token_type":"Bearer","expires_in":3600}
                """)));

        SfOAuthService service = buildService(wm.getHttpPort());
        SamlAssertionParams override = new SamlAssertionParams(
            "override-client", null, null, null, "override-tenant"
        );
        SfAccessToken token = service.generateToken(override);

        assertThat(token.accessToken()).isEqualTo("override-token");

        verify(postRequestedFor(urlEqualTo("/oauth/token"))
            .withRequestBody(containing("client_id=override-client"))
            .withRequestBody(containing("company_id=override-tenant")));
    }

    @Test
    void validateToken_shouldSendBearerHeaderAndReturnValidationResult(WireMockRuntimeInfo wm) {
        String rawBody = "{\"access_token\":\"sf-access-token\",\"token_type\":\"Bearer\",\"expires_in\":86312}";
        stubFor(get("/oauth/validate").willReturn(okJson(rawBody)));

        SfOAuthService service = buildService(wm.getHttpPort());
        String result = service.validateToken("sf-access-token");

        assertThat(result).isEqualTo(rawBody);
        verify(getRequestedFor(urlEqualTo("/oauth/validate"))
            .withHeader("Authorization", equalTo("Bearer sf-access-token")));
    }

    @Test
    void validateToken_shouldThrowWhenSapReturns401(WireMockRuntimeInfo wm) {
        stubFor(get("/oauth/validate")
            .willReturn(unauthorized()));

        SfOAuthService service = buildService(wm.getHttpPort());

        assertThatThrownBy(() -> service.validateToken("invalid-token"))
            .isInstanceOf(HttpClientErrorException.class);
    }

    private SfOAuthService buildService(int port) {
        SfProperties props = new SfProperties(
            "http://localhost:" + port,
            "test-client-id",
            "dummy-key",
            "test-tenant",
            "test-user",
            "dummy-cpi-url"
        );

        SamlAssertionService mockSaml = mock(SamlAssertionService.class);
        when(mockSaml.generateAssertion(any())).thenReturn(MOCK_ASSERTION);

        return new SfOAuthService(props, mockSaml, RestClient.builder());
    }
}
