package no.nav.nom.apps.nom.ressurser.dvh.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.ressurser.dvh.DvhOrdsResponseWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DvhOrdsClient {
    private static final int MAX_IN_MEMORY_MB = 300;
    private final WebClient webClient;

    public DvhOrdsClient(
        @Value("${dvh.ords-endpoint}") String ordsEndpoint,
        OAuth2AuthorizedClientManager authorizedClientManager,
        WebClient.Builder builder
    ) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("dvh-ords");

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(ordsEndpoint);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        this.webClient = builder.clone()
            .uriBuilderFactory(factory)
            .exchangeStrategies(
                ExchangeStrategies.builder().codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_MB * 1024 * 1024)).build()
            )
            .filter(oAuth2Filter)
            .filter(logRequest())
            .build();
    }

    public DvhOrdsResponseWrapper dvhGetDataLimitRows(int numRowsToFetch) {
        String filter = "{\"rownum\":{\"$lte\":" + numRowsToFetch + "}}";
        return dvhGetDataOrdsFilter(filter);
    }

    public DvhOrdsResponseWrapper dvhGetDataInSekvens(long fraSekvens, long tilOgMedSekvens) {
        String filter = "{\"pk_hrres_nom_historikk\": {\"$and\": [{\"$gt\":"+ fraSekvens + "}, {\"$lte\":" + tilOgMedSekvens + "}]}}";
        return dvhGetDataOrdsFilter(filter);
    }

    public DvhOrdsResponseWrapper dvhGetDataOrdsFilter(String ordsFilter) {
        return webClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("q", UriUtils.encodeQueryParam(ordsFilter, StandardCharsets.UTF_8))
                .build()
            )
            .retrieve()
            .bodyToMono(DvhOrdsResponseWrapper.class)
            .block();
    }

    public JsonNode dvhGetJsonDataLimitRows(int numRowsToFetch) {
        String filter = "{\"rownum\":{\"$lte\":" + numRowsToFetch + "}}";
        return dvhGetJsonDataOrdsFilter(filter);
    }

    public JsonNode dvhGetJsonDataOrdsFilter(String ordsFilter) {
        return webClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("q", UriUtils.encodeQueryParam(ordsFilter, StandardCharsets.UTF_8))
                .build()
            )
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }

    private ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            return next.exchange(clientRequest);
        };
    }
}