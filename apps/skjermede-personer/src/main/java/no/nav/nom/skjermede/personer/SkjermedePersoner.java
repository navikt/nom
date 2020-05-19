package no.nav.nom.skjermede.personer;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.UUID;

@EnableScheduling
@SpringBootApplication
@EnableJwtTokenValidation
public class SkjermedePersoner {
    public static void main(String[] args) {
        System.setProperty("kafka-transaction-uuid", UUID.randomUUID().toString().substring(0, 8));
        SpringApplication.run(SkjermedePersoner.class);
    }
}
