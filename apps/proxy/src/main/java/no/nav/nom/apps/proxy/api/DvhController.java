package no.nav.nom.apps.proxy.api;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.proxy.common.AppProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;


@Slf4j
@RestController
@RequestMapping("/proxy")
public class DvhController {

    private final AppProperties appProperties;
    private final WebClient webClient;
    private final UriBuilderFactory uriBuilderFactory;

    private final String PROXY_PATH = "/dvh";

    public DvhController(AppProperties appProperties, WebClient.Builder webClientBuilder) {
        this.appProperties = appProperties;
        this.webClient = webClientBuilder
            .baseUrl(appProperties.getDvh().getBaseUrl())
            .exchangeStrategies(useLargeInMemorySize())
            .filter(ExchangeFilterFunctions.basicAuthentication(appProperties.getDvh().getTdvUsername(), appProperties.getDvh().getTdvPassword()))
            .build();
        this.uriBuilderFactory = new DefaultUriBuilderFactory(appProperties.getDvh().getBaseUrl());
    }

    private ExchangeStrategies useLargeInMemorySize() {
        return ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs()
                .maxInMemorySize(appProperties.getWebClientMaxBytesInMemorySize()))
            .build();
    }

    @GetMapping(PROXY_PATH + "/**")
    public ResponseEntity<String> dvhProxyEndpoint(HttpServletRequest request) {
        String targetPath = request.getRequestURI().replaceFirst("/proxy" + PROXY_PATH, "");

        URI uri = uriBuilderFactory
            .uriString(targetPath)
            .query(request.getQueryString())
            .build();

        log.info("Mottatt forespørsel med path: " + request.getRequestURI() + ", ruter til: " + uri);

        return webClient.get()
            .uri(uri)
            .retrieve()
            .toEntity(String.class)
            .block();
    }

}
