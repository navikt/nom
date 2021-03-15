package no.nav.nom.skjermede.personer.pip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static no.nav.nom.libs.utils.common.properties.PropertyUtils.readVaultSrvProps;

@SpringBootApplication
public class ApplicationStarter {

    public static void main(String[] args) {
        readVaultSrvProps();
        SpringApplication.run(ApplicationStarter.class);

    }
}
