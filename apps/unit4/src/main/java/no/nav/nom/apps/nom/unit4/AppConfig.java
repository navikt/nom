package no.nav.nom.apps.nom.unit4;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationPropertiesScan
public class AppConfig {

    // Marker for use with logger to ensure that sensitive logs are filtered into the correct log
    public static final Marker SECURE_LOGS = MarkerFactory.getMarker("SECURE_LOGS");

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
