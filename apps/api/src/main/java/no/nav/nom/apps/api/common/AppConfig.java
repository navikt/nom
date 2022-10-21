package no.nav.nom.apps.api.common;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import javax.net.ssl.SSLException;

@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @Profile("proxy")
    public WebClientCustomizer insecureProxyWebClientCustomizer(
            @Value("${proxy.host:DEFAULT_NONE}") String proxyHost,
            @Value("${proxy.port:0}") Integer proxyPort
    ) throws SSLException {
        SslContext insecureContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        HttpClient httpClient = HttpClient.create()
                        .secure(spec -> spec.sslContext(insecureContext))
                        .proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP).host(proxyHost).port(proxyPort));

        return webClientBuilder -> webClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
