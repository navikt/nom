package no.nav.nom.libs.utils.common.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thomaskasene.wiremock.junit.WireMockStubs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WireMockStubs(LeaderElectionStub.class)
@SpringBootTest(
    classes = LeaderElectionTest.TestApplication.class,
    properties = {
        "unleash.enabled=false",
        "ELECTOR_PATH=localhost:${wiremock.server.port}/elect"
    }
)
public class LeaderElectionTest {

    @SpringBootApplication
    static class TestApplication {}

    private LeaderElectionStub electionStub;

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
        electionStub.setElectionResult(mapper.valueToTree(body));

        assertTrue(leaderElection.isLeader());
    }

    @Test
    void hostname_is_not_leader() throws Exception {
        final var body = Map.of(
            "name", "obviously_not_the_leader",
            "last_update", OffsetDateTime.now().toString()
        );
        electionStub.setElectionResult(mapper.valueToTree(body));

        assertFalse(leaderElection.isLeader());
    }

}
