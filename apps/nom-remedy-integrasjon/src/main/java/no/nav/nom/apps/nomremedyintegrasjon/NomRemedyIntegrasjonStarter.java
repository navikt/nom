package no.nav.nom.apps.nomremedyintegrasjon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NomRemedyIntegrasjonStarter {
    public static void main(String[] args){
        SpringApplication.run(NomRemedyIntegrasjonStarter.class, args);
    }
}
