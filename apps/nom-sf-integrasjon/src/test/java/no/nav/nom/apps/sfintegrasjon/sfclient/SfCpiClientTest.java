package no.nav.nom.apps.sfintegrasjon.sfclient;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.Leder;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.OverOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.orgenhet.domain.SfOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.outbox.SfOutboxDTO;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.UnknownContentTypeException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WireMockTest
class SfCpiClientTest {

    private static final String STI = "/http/NOM_to_Zalaris_Org_Data";
    private static final String TOKEN = "dummy-cpi-token";

    private static final SfOutboxDTO PAYLOAD = SfOutboxDTO.builder()
        .sporingsId("42")
        .endretAv(Set.of("Z990001"))
        .endringshendelser(Set.of("OrgEnhetKorrigertEventV1", "OrgEnhetLederOpprettetEventV1"))
        .sfOrgEnhet(SfOrgEnhet.builder()
            .id("aa0001")
            .navn("Seksjon for testing")
            .nivaa(NomNivaa.ARBEIDSOMRAADE)
            .erLovnode(true)
            .type(OrgEnhetsType.NAV_KONTOR)
            .tilgangsenhet("cc0003")
            .kostnadssted("0123400")
            .gyldigFom(LocalDate.of(2024, 1, 1))
            .gyldigTom(null)
            .ledere(List.of(new Leder("Z990002", LocalDate.of(2024, 2, 1), null)))
            .overOrgEnheter(List.of(new OverOrgEnhet("bb0002", NomNivaa.LINJEENHET, LocalDate.of(2024, 1, 1), null)))
            .build())
        .build();

    // JSON-en som sendes til CPI i dag. Endres et feltnavn, feiler testen, slik at endringen i kontrakten med Zalaris blir synlig.
    private static final String PAYLOAD_JSON = """
        {
          "sporingsId": "42",
          "endretAv": ["Z990001"],
          "endringshendelser": ["OrgEnhetKorrigertEventV1", "OrgEnhetLederOpprettetEventV1"],
          "sfOrgEnhet": {
            "id": "aa0001",
            "navn": "Seksjon for testing",
            "nivaa": "ARBEIDSOMRAADE",
            "erLovnode": true,
            "type": "NAV_KONTOR",
            "tilgangsenhet": "cc0003",
            "kostnadssted": "0123400",
            "gyldigFom": "2024-01-01",
            "gyldigTom": null,
            "ledere": [{"navident": "Z990002", "gyldigFom": "2024-02-01", "gyldigTom": null}],
            "overOrgEnheter": [{"id": "bb0002", "nivaa": "LINJEENHET", "gyldigFom": "2024-01-01", "gyldigTom": null}]
          }
        }
        """;

    private SfCpiClient sfCpiClient;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) {
        var authorizedClientManager = mock(OAuth2AuthorizedClientManager.class);
        when(authorizedClientManager.authorize(argThat(request -> "sap-cpi".equals(request.getClientRegistrationId()))))
            .thenReturn(autorisertKlient());

        // Base-URL uten skråstrek på slutten, slik som cpi-api-url i application.yaml
        var sfProperties = new SfProperties(
            "dummy-api-url",
            "test-client-id",
            "dummy-key",
            "test-tenant",
            "test-user",
            wm.getHttpBaseUrl(),
            LocalDate.now().minusDays(5),
            "TOPPNODE_ID"
        );
        sfCpiClient = new SfCpiClient(sfProperties, authorizedClientManager);
    }

    @Test
    @DisplayName("Sender payloaden som JSON med token for sap-cpi, og leser det speilede svaret")
    void senderPayloadOgLeserSpeiletSvar() {
        stubFor(post(STI).willReturn(okJson(PAYLOAD_JSON)));

        var respons = sfCpiClient.sendOrgenhetPayload(PAYLOAD);

        assertThat(respons.getBody()).isEqualTo(PAYLOAD);
        verify(postRequestedFor(urlEqualTo(STI))
            .withHeader("Authorization", equalTo("Bearer " + TOKEN))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(equalToJson(PAYLOAD_JSON, true, false)));
    }

    @Test
    @DisplayName("Svar uten body gir ResponseEntity uten body")
    void svarUtenBodyGirTomBody() {
        stubFor(post(STI).willReturn(ok()));

        assertThat(sfCpiClient.sendOrgenhetPayload(PAYLOAD).getBody()).isNull();
    }

    @Test
    @DisplayName("5xx fra CPI kastes som HttpServerErrorException med svaret fra CPI")
    void serverfeilKastes() {
        stubFor(post(STI).willReturn(serverError().withBody("CPI er nede")));

        assertThatThrownBy(() -> sfCpiClient.sendOrgenhetPayload(PAYLOAD))
            .isInstanceOfSatisfying(HttpServerErrorException.class, e -> assertThat(e.getResponseBodyAsString()).isEqualTo("CPI er nede"));
    }

    @Test
    @DisplayName("4xx fra CPI kastes som HttpClientErrorException")
    void klientfeilKastes() {
        stubFor(post(STI).willReturn(badRequest().withBody("Ugyldig payload")));

        assertThatThrownBy(() -> sfCpiClient.sendOrgenhetPayload(PAYLOAD))
            .isInstanceOf(HttpClientErrorException.class);
    }

    @Test
    @DisplayName("200 med noe annet enn JSON kastes som UnknownContentTypeException, ikke som nettverksfeil")
    void svarSomIkkeErJsonKastes() {
        stubFor(post(STI).willReturn(ok("<html>Logg inn</html>").withHeader("Content-Type", "text/html")));

        assertThatThrownBy(() -> sfCpiClient.sendOrgenhetPayload(PAYLOAD))
            .isInstanceOf(UnknownContentTypeException.class);
    }

    @Test
    @DisplayName("Brutt forbindelse kastes som ResourceAccessException")
    void bruttForbindelseKastes() {
        stubFor(post(STI).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> sfCpiClient.sendOrgenhetPayload(PAYLOAD))
            .isInstanceOf(ResourceAccessException.class);
    }

    private static OAuth2AuthorizedClient autorisertKlient() {
        var registrering = ClientRegistration.withRegistrationId("sap-cpi")
            .clientId("test-client-id")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://localhost/token")
            .build();
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, TOKEN, Instant.now(), Instant.now().plusSeconds(300));
        return new OAuth2AuthorizedClient(registrering, "anonymousUser", token);
    }
}
