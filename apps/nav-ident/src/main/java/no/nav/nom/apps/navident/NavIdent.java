package no.nav.nom.apps.navident;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.utils.common.properties.PropertyUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.UUID;

@SpringBootApplication
@EnableJpaRepositories
@ConfigurationPropertiesScan
@Slf4j
public class NavIdent {

    public static void main(String[] args) {
        PropertyUtils.initDefaultProperties("nav-ident");

        System.setProperty("spring.main.banner-mode", "log"); // Show customized banner
        SpringApplication.run(NavIdent.class, args);
    }
}