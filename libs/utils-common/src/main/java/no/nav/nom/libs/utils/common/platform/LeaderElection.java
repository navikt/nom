package no.nav.nom.libs.utils.common.platform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Schedulers;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class LeaderElection {

    public static final String ELECTOR_PATH_NAME = "ELECTOR_PATH";

    private final Environment environment;
    private final WebClient webClient;


    public LeaderElection(Environment environment, WebClient.Builder builder) {
        this.webClient = builder.clone()
            .baseUrl("http://" + environment.getRequiredProperty(ELECTOR_PATH_NAME))
            .build();
        this.environment = environment;
    }

    public boolean isLeader() {
        if (isLocal()) {
            return true;
        }
        return webClient.get()
            .retrieve()
            .bodyToMono(ElectionResult.class)
            .publishOn(Schedulers.boundedElastic())
            .map(electionResult -> {
                try {
                    return electionResult.isHostLeader(InetAddress.getLocalHost().getHostName());
                } catch (Exception e) {
                    log.error("Leader Election: error getting leaderName/hostName information", e);
                    return false;
                }
            })
            .blockOptional().orElseThrow();


    }

    private boolean isLocal() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return profiles.contains("local") || profiles.contains("test");
    }
}
