package no.nav.nom.apps.ui.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.ui.common.AppProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequiredArgsConstructor
class NomApiProxyController {
    private final RestTemplate restTemplate;
    private final AppProperties properties;
    private final ObjectMapper om;

    @SneakyThrows
    @PostMapping("api/v1/nom-api/graphql")
    ResponseEntity<String> query(@RequestBody String body) {
        val graphQlUrl = properties.getApi().getGraphQLUrl();
        val bodyNode = om.createObjectNode();
        bodyNode.put("query", body);
        val request = om.writeValueAsString(bodyNode);
        return restTemplate.postForEntity(graphQlUrl, request, String.class);
    }
}
