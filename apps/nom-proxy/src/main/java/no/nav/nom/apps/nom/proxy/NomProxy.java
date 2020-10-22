package no.nav.nom.apps.nom.proxy;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableJwtTokenValidation
@SpringBootApplication
public class NomProxy {
    public static void main(String[] args) {
        SpringApplication.run(NomProxy.class);
    }
}
