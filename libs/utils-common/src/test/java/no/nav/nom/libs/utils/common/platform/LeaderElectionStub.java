package no.nav.nom.libs.utils.common.platform;


import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

public class LeaderElectionStub extends WireMockExtension {

    public LeaderElectionStub(Builder builder) {
        super(builder);
    }

    public void setElectionResult(String body) {
        stubFor(
            get("/elect").willReturn(
                aResponse().withBody(body)
                    .withHeader("Content-Type", "application/json")
            )
        );
    }
}
