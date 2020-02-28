package no.nav.nom.skjermede.personer;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableJwtTokenValidation
public class SkjermedePersoner {
    public static void main(String[] args) {
        SpringApplication.run(SkjermedePersoner.class);

    }
}
