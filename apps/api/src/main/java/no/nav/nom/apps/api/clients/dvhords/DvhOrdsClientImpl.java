package no.nav.nom.apps.api.clients.dvhords;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Component("dvhOrdsClient")
@Slf4j
@Profile("!local")
public class DvhOrdsClientImpl implements DvhOrdsClient {
    private AppProperties.DvhOrds dvhOrdsProperties;
    private static final int MAX_IN_MEMORY_MB = 300;
    private final WebClient webClient;

    public DvhOrdsClientImpl(OAuth2AuthorizedClientManager authorizedClientManager, AppProperties appProperties) {
        dvhOrdsProperties = appProperties.clients().dvhOrds();

        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("dvh-ords");

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(dvhOrdsProperties.baseUrl());
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        this.webClient = WebClient.builder()
            .uriBuilderFactory(factory)
            .exchangeStrategies(
                ExchangeStrategies.builder().codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_MB * 1024 * 1024)).build()
            )
            .filter(oAuth2Filter)
            .filter(logRequest())
            .build();
    }

    @Override
    public JsonNode dvhGetJsonData(String urlPath) {
        return webClient
            .get()
            .uri(urlPath)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }

    @Override
    public JsonNode dvhGetJsonDataLimitRows(String urlPath, int numRowsToFetch) {
        String filter = "{\"rownum\":{\"$lte\":" + numRowsToFetch + "}}";
        return dvhGetJsonDataOrdsFilter(urlPath, filter);
    }

    @Override
    public JsonNode dvhGetJsonDataOrdsFilter(String urlPath, String ordsFilter) {
        return webClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path(urlPath)
                .queryParam("q", UriUtils.encodeQueryParam(ordsFilter, StandardCharsets.UTF_8))
                .build()
            )
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }

    @Override
    public JsonNode dvhGetJsonDataOrdsPaginated(String urlPath, int offset, int limit) {
        return webClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path(urlPath)
                .queryParam("offset", UriUtils.encodeQueryParam(Integer.toString(offset), StandardCharsets.UTF_8))
                .queryParam("limit", UriUtils.encodeQueryParam(Integer.toString(limit), StandardCharsets.UTF_8))
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