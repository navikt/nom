package no.nav.nom.apps.navident.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class Config {

    @Bean
    @Scope("singleton")
    public Map<String, Navident> navidenter() {
        return new ConcurrentHashMap<>();
    }
}
