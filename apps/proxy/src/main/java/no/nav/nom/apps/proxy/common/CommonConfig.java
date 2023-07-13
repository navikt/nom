package no.nav.nom.apps.proxy.common;

import io.micrometer.core.instrument.Tag;
import lombok.val;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTags;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTagsProvider;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@EnableScheduling
@Configuration(proxyBeanMethods = false)
public class CommonConfig {
    // Marker for use with logger to ensure that sensitive logs are filtered into the correct log
    public static final Marker SECURE_LOGS = MarkerFactory.getMarker("SECURE_LOGS");

    @Bean
    @Qualifier("restTemplateDvh")
    RestTemplate restTemplateDvh(RestTemplateBuilder builder, AppProperties properties) {
        return builder
                .additionalInterceptors(basicAuthInterceptor(properties.getDvh().getTdvUsername(), properties.getDvh().getTdvPassword()))
                .build();
    }

    @Bean
    @Primary
    RestTemplate restTemplate(){
        RestTemplate restTemplate = new RestTemplate();
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }


    // TODO RestTemplate is now instrumented automatically by Spring, so this can be removed. But need to analyze possible impact on dashboards and alerts
    @Bean
    RestTemplateExchangeTagsProvider restTemplateExchangeTagsProvider() {
        return (urlTemplate, request, response) -> {
            val uriTag = StringUtils.hasText(urlTemplate) ? RestTemplateExchangeTags.uri(urlTemplate.replaceFirst("\\?.*", "")) : Tag.of("uri", request.getURI().getPath());

            return Arrays.asList(RestTemplateExchangeTags.method(request), uriTag,
                    RestTemplateExchangeTags.status(response),
                    RestTemplateExchangeTags.clientName(request));
        };
    }

    private ClientHttpRequestInterceptor basicAuthInterceptor(String username, String password){
        return ((request, body, execution) -> {
            request.getHeaders().setBasicAuth(username, password);
            return  execution.execute(request, body);
        });
    }
}
