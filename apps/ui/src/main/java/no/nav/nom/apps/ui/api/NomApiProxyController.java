package no.nav.nom.apps.ui.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Set;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
@RequiredArgsConstructor
class NomApiProxyController extends AbstractAuthenticatedApi {
    private final RestTemplate restTemplate;
    private final ObjectMapper om;

    @SneakyThrows
    @PostMapping("api/v1/nom-api/graphql")
    ResponseEntity<String> query(@RequestBody String body, HttpServletRequest req) {
        val request = createRequest(body, req);

        return restTemplate.exchange(request, String.class);
    }

    @SneakyThrows
    private RequestEntity<String> createRequest(String body, HttpServletRequest req) {
        val obo = getOnBehalfOfToken(req, Set.of(properties.getNomApi().getClientId() + "/.default"));

        val bodyNode = om.createObjectNode();
        bodyNode.put("query", body);

        return RequestEntity.post(new URI(properties.getNomApi().getGraphQLUrl()))
                .header(AUTHORIZATION, "Bearer " + obo)
                .body(om.writeValueAsString(bodyNode));
    }
}
