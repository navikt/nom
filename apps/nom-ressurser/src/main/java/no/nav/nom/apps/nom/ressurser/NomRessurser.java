package no.nav.nom.apps.nom.ressurser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.UUID;

@EnableScheduling
@SpringBootApplication
public class NomRessurser {
    public static void main(String[] args) {
        System.setProperty("kafka-transaction-uuid", UUID.randomUUID().toString().substring(0, 8));
        SpringApplication.run(NomRessurser.class);

    }
}
