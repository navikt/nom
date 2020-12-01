package no.nav.nom.apps.ui.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequiredArgsConstructor
public class NomApiProxyController {
    private final RestTemplate restTemplate;

    @PostMapping("api/v1/nom-api/graphql")
    public ResponseEntity<String> query(@RequestBody String body) {
        return restTemplate.postForEntity("http://nom-api/graphql", body, String.class);
    }
}
