package no.nav.nom.apps.streams.kafkaapi.ressurs.hendelser;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.kafka")
public record AppProperties(
        String ressursEvtsrcTopic,
        String orgtilknytningEvtsrcTopic,
        String ressursHendelseTopic
) {
}
