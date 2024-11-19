package no.nav.nom.apps.rdbmsdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RdbmsDataStarter {
    public static void main(String[] args){
        SpringApplication.run(RdbmsDataStarter.class, args);
    }
}
