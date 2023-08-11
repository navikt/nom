package no.nav.nom.apps.proxy.api;

import no.nav.nom.apps.proxy.common.AppProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/proxy")
public class TelController {

    private final WebClient webClient;

    public TelController(AppProperties appProperties) {
        this.webClient = WebClient.builder()
            .baseUrl(appProperties.getTel().getUrl())
            .filter(ExchangeFilterFunctions
                .basicAuthentication(appProperties.getTel().getTelUsername(), appProperties.getTel().getTelPassword()))
            .build();
    }

    @GetMapping("/tel")
    public ResponseEntity<String> getTelv1() {
        return webClient.get()
            .retrieve()
            .toEntity(String.class)
            .block();
    }
}
