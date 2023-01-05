package no.nav.nom.libs.utils.common.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.thomaskasene.wiremock.junit.WireMockStub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

public class LeaderElectionStub implements WireMockStub {
    public void setElectionResult(JsonNode body) {
        stubFor(
            get("/elect").willReturn(
                aResponse().withJsonBody(body)
                    .withHeader("Content-Type", "application/json")
            )
        );
    }
}
