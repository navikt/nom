package no.nav.nom.skjermede.personer;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.UUID;

import static no.nav.nom.libs.utils.common.properties.PropertyUtils.readVaultSrvProps;

@EnableScheduling
@SpringBootApplication
@EnableJwtTokenValidation(ignore = {"org.springframework", "springfox"})
public class SkjermedePersoner {

    public static void main(String[] args) {
        readVaultSrvProps();
        System.setProperty("kafka-transaction-uuid", UUID.randomUUID().toString().substring(0, 8));
        SpringApplication.run(SkjermedePersoner.class);
        
    }
}
