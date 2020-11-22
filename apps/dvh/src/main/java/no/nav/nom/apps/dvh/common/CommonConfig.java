package no.nav.nom.apps.dvh.common;

import io.micrometer.core.instrument.Tag;
import lombok.val;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTags;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.reactive.client.WebClientExchangeTags;
import org.springframework.boot.actuate.metrics.web.reactive.client.WebClientExchangeTagsProvider;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;

@EnableKafka
@EnableScheduling
@Configuration(proxyBeanMethods = false)
class CommonConfig {

    @Bean
    WebClientExchangeTagsProvider webClientExchangeTagsProvider() {
        return (request, response, throwable) ->
            Arrays.asList(
                    WebClientExchangeTags.method(request),
                    WebClientExchangeTags.uri(request),
                    WebClientExchangeTags.status(response, throwable),
                    WebClientExchangeTags.clientName(request));
    }

    @Bean(name = "enhetWebClient")
    WebClient getEnhetWebClient(AppProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.getDvh().getEnhet().getUrl())
                .build();
    }

    @Bean(name = "ressursWebClient")
    WebClient getRessursWebClient(AppProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.getDvh().getRessurs().getUrl())
                .build();
    }
}
