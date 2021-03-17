package no.nav.nom.skjermede.personer.pip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import static no.nav.nom.libs.utils.common.properties.PropertyUtils.readVaultSrvProps;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApplicationStarter {

    public static void main(String[] args) {
        readVaultSrvProps();
        SpringApplication.run(ApplicationStarter.class);

    }
}
