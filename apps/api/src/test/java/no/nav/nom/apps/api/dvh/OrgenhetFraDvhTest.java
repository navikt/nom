package no.nav.nom.apps.api.dvh;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

public class OrgenhetFraDvhTest extends DvhTestBase {

    /*@Test
    @DisplayName("Orgenhet fra datavarehus med ikke-aktiv status ignoreres")
    void test1() {
        stubEnheter("responses/dvhdiff/dvhOrgenhet2_Pre.json");
        loadFromDvh();

        var alle = orgenhetService.getAll();
        assertThat(alle).hasSize(0);
    }

    @Test
    @DisplayName("Nav ventelønn og deltar i rekruttering ignoreres")
    void test2() {
        stubEnheter("responses/dvhdiff/dvhOrgenhet3_Pre.json");
        loadFromDvh();

        var alle = orgenhetService.getAll();
        assertThat(alle).hasSize(2);
    }*/

    private void stubEnheter(String fileName) {
        stubFor(get("/proxy/dvh/json/Hr/NOM2/NOM_HRORG_HIERARKI2_v2").willReturn(aResponse().withStatus(200)
                                                                                         .withHeader("Content-Type", "application/json;charset=UTF-8")
                                                                                         .withBodyFile(fileName)));
    }

}
