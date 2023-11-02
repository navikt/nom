package no.nav.nom.libs.utils.common.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest(
    classes = LeaderElectionTest.TestApplication.class,
    properties = {
        "unleash.enabled=false",
        "ELECTOR_PATH=localhost:9797/elect"
    }
)
public class LeaderElectionTest {

    @SpringBootApplication
    static class TestApplication {}


    @RegisterExtension
    static LeaderElectionStub electionStub = new LeaderElectionStub(WireMockExtension.extensionOptions()
        .options(wireMockConfig().port(9797))
        .configureStaticDsl(true));

    @Autowired
    LeaderElection leaderElection;

    @Autowired
    ObjectMapper mapper;

    @Test
    void contextLoads() {}

    @Test
    void hostname_is_leader() throws Exception {
        final var body = Map.of(
            "name", InetAddress.getLocalHost().getHostName(),
            "last_update", OffsetDateTime.now().toString()
        );
        electionStub.setElectionResult(mapper.writeValueAsString(body));

        assertTrue(leaderElection.isLeader());
    }

    @Test
    void hostname_is_not_leader() throws Exception {
        final var body = Map.of(
            "name", "obviously_not_the_leader",
            "last_update", OffsetDateTime.now().toString()
        );
        electionStub.setElectionResult(mapper.writeValueAsString(body));

        assertFalse(leaderElection.isLeader());
    }

}
