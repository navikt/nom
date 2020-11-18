package no.nav.nom.libs.utils.common.properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
public class PropertiesAutoConfig {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(value = "nom.libs.utils.common.app.load-properties", matchIfMissing = true)
    @PropertySource(value = "common-app.yaml", factory = YamlPropertySourceFactory.class)
    public static class CommonAppConfig {

    }
}
