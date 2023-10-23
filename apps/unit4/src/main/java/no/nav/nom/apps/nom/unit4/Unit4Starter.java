package no.nav.nom.apps.nom.unit4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class Unit4Starter {

    public static void main(String[] args) {
        SpringApplication.run(Unit4Starter.class, args);
    }

}