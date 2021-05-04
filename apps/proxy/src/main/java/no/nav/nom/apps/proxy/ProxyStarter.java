package no.nav.nom.apps.proxy;

import no.nav.nom.libs.utils.common.properties.PropertyUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProxyStarter {
    public static void main(String[] args) {
        PropertyUtils.initDefaultProperties("nom-proxy");
        SpringApplication.run(ProxyStarter.class, args);
        
    }
}
