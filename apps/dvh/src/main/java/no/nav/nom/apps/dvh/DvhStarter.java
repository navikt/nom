package no.nav.nom.apps.dvh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DvhStarter {
    public static void main(String[] args) {
        System.setProperty("app.name", "nom-dvh");
        SpringApplication.run(DvhStarter.class, args);
    }
}
