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

@RestController
@RequiredArgsConstructor
class NomApiProxyController extends AbstractAuthenticatedApi {
    private final RestTemplate restTemplate;
    private final ObjectMapper om;

    @SneakyThrows
    @PostMapping("api/v1/nom-api/graphql")
    ResponseEntity<String> query(@RequestBody String body, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        rotateAccessKeyIfExpired(servletRequest, servletResponse);

        val request = createRequest(body);

        return restTemplate.exchange(request, String.class);
    }

    @SneakyThrows
    private RequestEntity<String> createRequest(String body) {

        val bodyNode = om.createObjectNode();
        bodyNode.put("query", body);
        val requestJson = om.writeValueAsString(bodyNode);
        return new RequestEntity<>(requestJson, HttpMethod.POST, new URI(properties.getNomApi().getGraphQLUrl()));
    }
}
