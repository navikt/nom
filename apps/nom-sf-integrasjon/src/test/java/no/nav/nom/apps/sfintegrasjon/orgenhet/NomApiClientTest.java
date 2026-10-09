package no.nav.nom.apps.sfintegrasjon.orgenhet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NomApiClientTest {

    private static final String KOSTNADSSTED_URL = "http://nom-api/app/orgenheter/kostnadssted/aa0001";

    private MockRestServiceServer server;
    private NomApiClient nomApiClient;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://nom-api");
        server = MockRestServiceServer.bindTo(builder).build();
        nomApiClient = new NomApiClient(builder.build());
    }

    @Test
    @DisplayName("Returnerer kostnadsstedet til orgenheten")
    void returnererKostnadssted() {
        server.expect(requestTo(KOSTNADSSTED_URL))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {"kostnadsstedId": "0123400", "orgenhetId": "aa0001", "gyldigFom": "2024-01-01", "gyldigTom": null}
                """, MediaType.APPLICATION_JSON));

        assertThat(nomApiClient.getKostnadsstedForOrgEnhet("aa0001"))
            .contains(new OrgEnhetKostnadsstedMappingResponse("0123400", "aa0001", LocalDate.of(2024, 1, 1), null));
        server.verify();
    }

    @Test
    @DisplayName("404 betyr at orgenheten mangler kostnadssted, og gir tomt svar")
    void ikkeFunnetGirTomtSvar() {
        server.expect(requestTo(KOSTNADSSTED_URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(nomApiClient.getKostnadsstedForOrgEnhet("aa0001")).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("Andre klientfeil enn 404 kastes videre, slik at de ikke tolkes som manglende kostnadssted")
    void andreKlientfeilKastesVidere() {
        server.expect(requestTo(KOSTNADSSTED_URL)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> nomApiClient.getKostnadsstedForOrgEnhet("aa0001"))
            .isInstanceOf(HttpClientErrorException.Forbidden.class);
    }

    @Test
    @DisplayName("Serverfeil kastes videre")
    void serverfeilKastesVidere() {
        server.expect(requestTo(KOSTNADSSTED_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> nomApiClient.getKostnadsstedForOrgEnhet("aa0001"))
            .isInstanceOf(HttpServerErrorException.class);
    }
}
