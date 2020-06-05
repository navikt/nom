package no.nav.nom.skjermede.personer.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderElector {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public boolean isLeader() {
        try {
            val electorPath = System.getenv("ELECTOR_PATH");
            val response = restTemplate.getForObject(String.format("http://%s", electorPath), String.class);
            val leaderName = objectMapper.readTree(response).get("name").asText();
            val hostname = InetAddress.getLocalHost().getHostName();

            return leaderName.equals(hostname);
        } catch (Exception e) {
            if (Arrays.asList(environment.getActiveProfiles()).contains("local")){
                return true;
            };

            log.error("Leader Election: error getting leaderName/hostName information", e);
        }

        return false;
    }
}