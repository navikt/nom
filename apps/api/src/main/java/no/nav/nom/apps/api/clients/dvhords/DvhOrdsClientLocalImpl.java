package no.nav.nom.apps.api.clients.dvhords;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;

@Component("dvhOrdsClient")
@Slf4j
@Profile("local")
public class DvhOrdsClientLocalImpl implements DvhOrdsClient {

    private final RestClient restClient;

    public DvhOrdsClientLocalImpl(OAuth2AuthorizedClientManager authorizedClientManager, AppProperties appProperties, RestClient.Builder builder) {
        AppProperties.DvhOrds dvhOrdsProperties = appProperties.clients().dvhOrds();

        OAuth2ClientHttpRequestInterceptor oauth2Interceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        oauth2Interceptor.setClientRegistrationIdResolver(request -> "nom-api");

        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(dvhOrdsProperties.baseUrl());
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        this.restClient = builder
            .uriBuilderFactory(uriBuilderFactory)
            .requestInterceptor(oauth2Interceptor)
            .requestInterceptor((request, body, execution) -> {
                log.info("Request: {} {}", request.getMethod(), request.getURI());
                return execution.execute(request, body);
            })
            .build();
    }

    @Override
    public JsonNode dvhGetJsonData(String urlPath) {
        return restClient.get()
            .uri(urlPath)
            .retrieve()
            .body(JsonNode.class);
    }

    @Override
    public JsonNode dvhGetJsonDataLimitRows(String urlPath, int numRowsToFetch) {
        return restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(urlPath)
                .queryParam("numRows", numRowsToFetch)
                .build()
            )
            .retrieve()
            .body(JsonNode.class);
    }

    @Override
    public JsonNode dvhGetJsonDataOrdsFilter(String urlPath, String ordsFilter) {
        return restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(urlPath)
                .queryParam("ordsFilter", UriUtils.encodeQueryParam(ordsFilter, StandardCharsets.UTF_8))
                .build()
            )
            .retrieve()
            .body(JsonNode.class);
    }

    @Override
    public JsonNode dvhGetJsonDataOrdsPaginated(String urlPath, int offset, int limit) {
        return restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(urlPath.replace("?numRows=0", "Paginated"))
                .queryParam("offset", UriUtils.encodeQueryParam(Integer.toString(offset), StandardCharsets.UTF_8))
                .queryParam("limit", UriUtils.encodeQueryParam(Integer.toString(limit), StandardCharsets.UTF_8))
                .build()
            )
            .retrieve()
            .body(JsonNode.class);
    }
}
