package no.nav.nom.libs.utils.common.platform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class LeaderElection {

    public static final String ELECTOR_PATH_NAME = "ELECTOR_PATH";

    private final RestTemplate restTemplate;
    private final Environment environment;

    private final String electorPath;

    public LeaderElection(RestTemplateBuilder restTemplateBuilder, Environment environment) {
        this.restTemplate = restTemplateBuilder.build();
        this.environment = environment;
        this.electorPath = "http://" + environment.getRequiredProperty(ELECTOR_PATH_NAME);
    }

    public boolean isLeader() {
        if (isLocal()) {
            return true;
        }

        try {
            ElectionResult election = restTemplate.getForObject(electorPath, ElectionResult.class);
            return election.isHostLeader(InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            log.error("Leader Election: error getting leaderName/hostName information", e);
            return false;
        }
    }

    private boolean isLocal() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return profiles.contains("local") || profiles.contains("test");
    }
}
