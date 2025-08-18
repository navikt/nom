package no.nav.nom.apps.streams.kafkaapi.ressurs.state;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.kafka")
public record AppProperties(
    String stateTopic,
    String ressursEvtsrcTopic,
    String orgtilknytningEvtsrcTopic,
    String navidentTopic,
    int skipProducingStateEventsUntilRessursEvtsrcOffset,
    int skipProducingStateEventsUntilOrgtilknytningEvtsrcOffset
) {}
