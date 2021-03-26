package no.nav.nom.libs.utils.common.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class LeaderElection {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Environment environment;

    public boolean isLeader() {
        if (isLocal()) {
            return true;
        }
        String electorPath = System.getenv("ELECTOR_PATH");
        String leaderName;
        String hostname;

        try {
            String response = restTemplate.getForObject("http://" + electorPath, String.class);
            leaderName = objectMapper.readTree(response).get("name").asText();
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            log.error("Leader Election: error getting leaderName/hostName information", e);
            return false;
        }
        return leaderName.equals(hostname);
    }

    private boolean isLocal() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return profiles.contains("local") || profiles.contains("test");
    }
}
