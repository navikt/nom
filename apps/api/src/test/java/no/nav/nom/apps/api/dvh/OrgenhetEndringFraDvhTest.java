package no.nav.nom.apps.api.dvh;

import no.nav.nom.apps.api.orgenhet.EventBasedOrgenhet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

public class OrgenhetEndringFraDvhTest extends DvhTestBase {

    @Test
    @DisplayName("Orgenhet fra datavarehus blir tilgjengelige i orgenhet repo")
    void test1() {
        stubEnheter("responses/dvhdiff/dvhOrgenhet1_Pre.json");
        loadFromDvh();

        var alle = orgenhetService.getAll();
        assertThat(alle).hasSize(5);
        alle.forEach(org -> assertThat(org.getNavn()).isEqualTo("testNavn"));
    }

    @Test
    @DisplayName("Orgenhet fra datavarehus oppdaterer navn, manglende enheter røres ikke")
    void test2() {
        stubEnheter("responses/dvhdiff/dvhOrgenhet1_Pre.json");
        loadFromDvh();
        stubEnheter("responses/dvhdiff/dvhOrgenhet1_Post.json");
        loadFromDvh();

        var alle = orgenhetService.getAll();
        assertThat(alle).hasSize(6);
        alle.sort(Comparator.comparing(EventBasedOrgenhet::getAgressoId));

        assertThat(alle.get(0).getNavn()).isEqualTo("testNavn");
        assertThat(alle.get(1).getNavn()).isEqualTo("testNavn");
        assertThat(alle.get(2).getNavn()).isEqualTo("testNavn2");
        assertThat(alle.get(3).getNavn()).isEqualTo("testNavn3");
        assertThat(alle.get(4).getNavn()).isEqualTo("testNavn");
        assertThat(alle.get(5).getNavn()).isEqualTo("testNavn");
    }

    private void stubEnheter(String fileName) {
        stubFor(get("/proxy/enheter/v2").willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json;charset=UTF-8")
                .withBodyFile(fileName)
        ));
    }

}
