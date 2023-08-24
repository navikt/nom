package no.nav.nom.libs.utils.common.platform;


import com.thomaskasene.wiremock.junit.WireMockStub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

public class LeaderElectionStub implements WireMockStub {
    public void setElectionResult(String body) {
        stubFor(
            get("/elect").willReturn(
                aResponse().withBody(body)
                    .withHeader("Content-Type", "application/json")
            )
        );
    }
}
