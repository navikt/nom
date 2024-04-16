package no.nav.nom.libs.state.properties;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.utils.common.properties.YamlPropertySourceFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class PropertiesAutoConfig {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(value = "nom.libs.state.ressurs.enabled")
    @PropertySource(value = "ressurs-state.yaml", factory = YamlPropertySourceFactory.class)
    public static class RessursStateConfig {
        public RessursStateConfig() {
            log.debug("loading ressurs-state.yaml");
        }
    }
}
