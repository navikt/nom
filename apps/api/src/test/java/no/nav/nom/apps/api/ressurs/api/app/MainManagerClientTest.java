package no.nav.nom.apps.api.ressurs.api.app;

import no.nav.nom.apps.api.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

public class MainManagerClientTest extends TestBase {

    @Autowired
    private MainManagerClient mainManagerClient;

    @Test
    @DisplayName("getFysiskeBrukersteder should fetch and map sites from MainManager")
    void testGetFysiskeBrukersteder() throws Exception {
        var body = new ClassPathResource("mainmanager/sites.json")
            .getContentAsString(StandardCharsets.UTF_8);

        stubFor(
            get(urlEqualTo("/api/v2/sites"))
                .willReturn(
                    aResponse()
                        .withBody(body)
                        .withHeader("Content-Type", "application/json")
                )
        );

        var sites = mainManagerClient.getFysiskeBrukersteder();

        assertThat(sites).isNotEmpty();
        assertThat(sites.getFirst().navn()).isEqualTo("Dovre, Kyrkjevegen 18");
        assertThat(sites.getFirst().address()).isEqualTo("Kyrkjevegen 18");
        assertThat(sites.getFirst().postnummer()).isEqualTo("2660");
        assertThat(sites.getFirst().poststed()).isEqualTo("DOMBÅS");
        assertThat(sites.getFirst().kommune()).isEqualTo("Dovre kommune");
        assertThat(sites.getFirst().nr()).isNull();

        verify(getRequestedFor(urlEqualTo("/api/v2/sites")));
    }
}

