package no.nav.nom.libs.utils.common.properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration(proxyBeanMethods = false)
public class PropertiesAutoConfig {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(value = "nom.libs.utils.common.app.load-properties", matchIfMissing = true)
    @PropertySource(value = "common-app.yaml", factory = YamlPropertySourceFactory.class)
    public static class CommonAppConfig {

    }

    @Profile("kafka")
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnProperty(value = "nom.libs.utils.common.kafka.load-properties", matchIfMissing = true)
    @PropertySource(value = "common-kafka.yaml", factory = YamlPropertySourceFactory.class)
    public static class CommonKafkaConfig {

    }
}
