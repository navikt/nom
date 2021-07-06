package no.nav.nom.apps.api.dvh;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

public class OrgenhetFraDvhTest extends DvhTestBase {

    @Test
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
    }

    private void stubEnheter(String fileName) {
        stubFor(get("/proxy/enheter/v2").willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json;charset=UTF-8")
                .withBodyFile(fileName)
        ));
    }

}
