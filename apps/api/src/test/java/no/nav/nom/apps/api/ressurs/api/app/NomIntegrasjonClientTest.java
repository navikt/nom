package no.nav.nom.apps.api.ressurs.api.app;

import no.nav.nom.apps.api.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

public class NomIntegrasjonClientTest extends TestBase {

    @Autowired
    private NomRemedyIntegrasjonClient nomRemedyIntegrasjonClient;

    @Test
    @DisplayName("getFysiskeBrukerstedNrs should perform request as expected")
    void testGetFysiskeBrukerstedNrs() {
        stubFor(
            get(anyUrl())
                .willReturn(
                    aResponse()
                        .withBody("""
                                  [{"brukerstedNr": "1234", "brukerstedNavn": "Mordor"}]""")
                        .withHeader("Content-Type", "application/json")
                )
        );

        assertThat(nomRemedyIntegrasjonClient.getFysiskeBrukerstedNrs(List.of("1234", "4321")))
            .isNotNull();

        verify(
            getRequestedFor(
                urlEqualTo(
                    "/api/fysiskebrukersteder?fysiskeBrukerstedNrs=1234&fysiskeBrukerstedNrs=4321"
                )
            )
        );
    }
}
