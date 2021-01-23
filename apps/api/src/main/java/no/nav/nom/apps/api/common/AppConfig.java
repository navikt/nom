package no.nav.nom.apps.api.common;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.ProxyProvider;

@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate(){ return new RestTemplate(); }

    @Bean
    @Profile("!proxy")
    @ConditionalOnMissingBean
    public WebClient webClient(){ return WebClient.create(); }

    @Value( "${proxy.host:DEFAULT_NONE}" )
    private String proxyHost;
    @Value( "${proxy.port:0}" )
    private Integer proxyPort;

    @Bean
    @Profile("proxy")
    public WebClient insecureProxyWebClient() {
        HttpClient httpClient =
                HttpClient.create()
                        .secure( spec -> {
                            spec.sslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE));
                        })
                        .tcpConfiguration(tcpClient ->
                                tcpClient.proxy(proxy -> {
                                    proxy.type(ProxyProvider.Proxy.HTTP).host(proxyHost).port(proxyPort);
                                })
                        );
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        return WebClient.builder().clientConnector(connector).build();
    }
}
