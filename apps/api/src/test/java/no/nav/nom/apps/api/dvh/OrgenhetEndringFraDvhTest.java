package no.nav.nom.apps.api.dvh;

import no.nav.nom.apps.api.domain.Orgenhet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

public class OrgenhetEndringFraDvhTest extends DvhTestBase {

    @Test
    @DisplayName("Orgenhet fra datavarehus blir tilgjengelige i orgenhet repo")
    void test1() {
        simulateSyncedWithKafka();
        stubEnheter("responses/dvhdiff/dvhOrgenhet1_Pre.json");
        loadFromDvhAndUpdateRepo();

        var alle = this.orgenhetRepository.getAlleOrgEnhetById().values();
        assertThat(alle).hasSize(5);
        alle.forEach(org -> assertThat(org.getNavn()).isEqualTo("testNavn"));
    }

    @Test
    @DisplayName("Orgenhet fra datavarehus oppdaterer navn, manglende enheter røres ikke")
    void test2() {
        simulateSyncedWithKafka();
        stubEnheter("responses/dvhdiff/dvhOrgenhet1_Pre.json");
        loadFromDvhAndUpdateRepo();
        stubEnheter("responses/dvhdiff/dvhOrgenhet1_Post.json");
        loadFromDvhAndUpdateRepo();

        var alle = new ArrayList<>(this.orgenhetRepository.getAlleOrgEnhetById().values());
        assertThat(alle).hasSize(6);
        alle.sort(Comparator.comparing(Orgenhet::getAgressoId));

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
