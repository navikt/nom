package no.nav.nom.apps.dvh.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Profile("kafka")
@Configuration(proxyBeanMethods = false)
class KafkaConfig {

}
