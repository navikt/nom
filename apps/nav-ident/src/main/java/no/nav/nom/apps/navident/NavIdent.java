package no.nav.nom.apps.navident;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.utils.common.properties.PropertyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@ConfigurationPropertiesScan
@Slf4j
public class NavIdent {

    private final Environment env;

    public NavIdent(Environment env) {
        this.env = env;

        log.info("DATABASE_DATABASE env prop: {}", env.getProperty("DATABASE_DATABASE"));
        log.info("DATABASE_DATABASE env prop: {}", env.getProperty("DATABASE.DATABASE"));
        log.info("NAIS_CLIENT_ID env prop: {}", env.getProperty("NAIS_CLIENT_ID"));
        log.info("NAIS_CLIENT_ID env prop: {}", env.getProperty("NAIS.CLIENT.ID"));
    }

    public static void main(String[] args) {
        PropertyUtils.initDefaultProperties("nav-ident");
        System.setProperty("spring.main.banner-mode", "log"); // Show customized banner

        log.info("In main: URL -> {}", System.getProperty("spring.datasource.url"));
        log.info("DATABASE_DATABASE system prop: {}", System.getProperty("DATABASE_DATABASE"));

        SpringApplication.run(NavIdent.class, args);
    }
}