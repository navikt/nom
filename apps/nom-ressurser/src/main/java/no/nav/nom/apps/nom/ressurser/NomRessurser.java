package no.nav.nom.apps.nom.ressurser;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
@EnableJwtTokenValidation
public class NomRessurser {
    public static void main(String[] args) {
        System.setProperty("kafka-transaction-uuid", UUID.randomUUID().toString().substring(0, 8));
        SpringApplication.run(NomRessurser.class);
    }
}
