package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiRessursStateStarter {
    public static void main(String[] args) {
        SpringApplication.run(ApiRessursStateStarter.class, args);
    }
}
