package no.nav.nom.apps.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaRepositories
@EnableAsync
@ConfigurationPropertiesScan
public class ApiStarter {
    public static void main(String[] args) {
        SpringApplication.run(ApiStarter.class, args);
    }
}