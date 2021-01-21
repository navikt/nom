package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.dto.DvhKobling;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class DvhRestConsumer {
    private final long ONE_HOUR = 60 * 60 * 1000;

    private final AadTokenRepository aadTokenRepository;
    private final DvhRepository dvhRepository;
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    @Scheduled(fixedDelay = ONE_HOUR)
    public void loadKoblingerFromDvh(){
        val headers = new HttpHeaders();
        headers.setBearerAuth(aadTokenRepository.getDvhToken());

        val result = restTemplate.exchange(appProperties.getDvh().getBaseUrl()+"/proxy/stillinger", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();

        dvhRepository.clearKoblinger();
        val arrNode = (ArrayNode) result.get("stilling2nomResponse").get("stilling2nomResult");
        for (JsonNode jsonNode : arrNode) {
            ObjectNode x = (ObjectNode) jsonNode;
            DvhKobling kobling = new DvhKobling(x);
            dvhRepository.saveKobling(kobling);
        }
    }
}
