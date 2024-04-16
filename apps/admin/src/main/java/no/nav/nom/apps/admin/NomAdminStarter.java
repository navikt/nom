package no.nav.nom.apps.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NomAdminStarter {
    public static void main(String[] args){
        SpringApplication.run(NomAdminStarter.class, args);
    }
}
