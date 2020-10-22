package no.nav.nom.apps.nom.enheter.common;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@EnableOAuth2Client(cacheEnabled = true)
@Configuration
public class RestTemplateConfig {

    @Qualifier(value = "nomProxyRestTemplate")
    @Bean
    public RestTemplate nomProxyRestTemplate(
            RestTemplateBuilder restTemplateBuilder,
            ClientConfigurationProperties clientConfigurationProperties,
            OAuth2AccessTokenService oAuth2AccessTokenService
    ) {

        ClientProperties clientProperties =
                Optional.ofNullable(clientConfigurationProperties.getRegistration().get("nom-proxy-client"))
                        .orElseThrow(() -> new RuntimeException("could not find oauth2 client config for nom-proxy-client"));

        RestTemplate template = restTemplateBuilder
                .additionalInterceptors(bearerTokenInterceptor(clientProperties, oAuth2AccessTokenService))
                .build();
        return template;
    }

    private ClientHttpRequestInterceptor bearerTokenInterceptor(
            ClientProperties clientProperties,
            OAuth2AccessTokenService oAuth2AccessTokenService
    ) {
        return (request, body, execution) -> {
            OAuth2AccessTokenResponse response =
                    oAuth2AccessTokenService.getAccessToken(clientProperties);
            log.info("token: "+response.getAccessToken());
            request.getHeaders().setBearerAuth(response.getAccessToken());
            return execution.execute(request, body);
        };
    }
}
