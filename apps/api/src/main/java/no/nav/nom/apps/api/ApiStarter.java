package no.nav.nom.apps.api;

import no.nav.nom.libs.utils.common.properties.PropertyUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@ConfigurationPropertiesScan
public class ApiStarter {
    public static void main(String[] args) {
        PropertyUtils.initDefaultProperties("nom-api");
        SpringApplication.run(ApiStarter.class, args);

    }
}