package no.nav.nom.apps.sfintegrasjon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NomSfIntegrasjonApplication {
    public static void main(String[] args) {
        SpringApplication.run(NomSfIntegrasjonApplication.class, args);
    }
}
