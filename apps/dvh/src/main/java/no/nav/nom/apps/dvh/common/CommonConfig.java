package no.nav.nom.apps.dvh.common;

import io.micrometer.core.instrument.Tag;
import lombok.val;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTags;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTagsProvider;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@EnableKafka
@EnableScheduling
@Configuration(proxyBeanMethods = false)
class CommonConfig {

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder, AppProperties properties) {
        return builder
                .additionalInterceptors(basicAuthInterceptor(properties.getDvh().getTdvUsername(), properties.getDvh().getTdvPassword()))
                .build();
    }

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
