package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiRessursHendelserStarter {

    public static void main(String[] args) {
        SpringApplication.run(ApiRessursHendelserStarter.class, args);
    }

}
