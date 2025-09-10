package no.nav.nom.apps.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan("no.nav.nom.apps.admin,no.nav.nom.libs.utils.common.excel")
public class NomAdminStarter {
    public static void main(String[] args) {
        SpringApplication.run(NomAdminStarter.class, args);
    }
}
