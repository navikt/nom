package no.nav.nom.apps.proxy.api;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.proxy.common.AppProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import jakarta.servlet.http.HttpServletRequest;

import reactor.core.publisher.Mono;

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
            .filter(logResponse())
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
    public ResponseEntity<String> dvhProxyEndpoint(HttpServletRequest request, JwtAuthenticationToken token) {
        String targetPath = request.getRequestURI().replaceFirst("/proxy" + PROXY_PATH, "");

        URI uri = uriBuilderFactory
            .uriString(targetPath)
            .query(request.getQueryString())
            .build();


        var appName = "ukjent";
        if (token != null) {
            var tokenAtts = token.getTokenAttributes();
            appName = tokenAtts.getOrDefault("azp_name", tokenAtts.getOrDefault("azp", appName)).toString();
        }

        log.info("Mottatt forespørsel fra [" + appName + "] med path: " + request.getRequestURI() + ", ruter til: " + uri);

        // NOTE: attempt to fix duplicate headers
        var clientResponse = webClient.get()
            .uri(uri)
            .retrieve()
            .toEntity(String.class)
            .block();
        assert clientResponse != null;
        return ResponseEntity
            .status(clientResponse.getStatusCode())
            .contentType(MediaType.APPLICATION_JSON)
            .body(clientResponse.getBody());

        /*
        return webClient.get()
            .uri(uri)
            .retrieve()
            .toEntity(String.class)
            .block();
        */
    }

    ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder("NOM-PROXY Response status and headers: \n")
                    .append("Status: ")
                    .append(clientResponse.statusCode());
                clientResponse
                    .headers()
                    .asHttpHeaders()
                    .forEach((key, value1) -> value1.forEach(value -> sb
                        .append("\n")
                        .append(key)
                        .append(":")
                        .append(value)));
                log.debug(sb.toString());
            }
            return Mono.just(clientResponse);
        });
    }

}
