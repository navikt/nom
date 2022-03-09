package no.nav.nom.apps.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class UiStarter {

    public static void main(String[] args) {
        System.setProperty("app.name", "nom-api");
        SpringApplication.run(UiStarter.class, args);
        
    }
}
