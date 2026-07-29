package no.nav.nom.apps.sfintegrasjon.organisasjon.eventsource;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka.evtsrc")
public class EvtsrcKafkaProperties {
    private String groupId;
    private boolean autoStartup = true;
    /** Ventetid i ms mellom første og andre state-oppslag ved race condition. Default: 2000ms. */
    private long retryDelayMs = 2_000;
    private TopicConfig orgenhet = new TopicConfig();
    private TopicConfig orgenhetLeder = new TopicConfig();
    private TopicConfig organisering = new TopicConfig();

    @Getter
    @Setter
    public static class TopicConfig {
        private String topic;
        /**
         * Startoffset ved første oppstart (ingen committed offsets for denne partisjonen).
         * -1 = start fra latest (auto.offset.reset=latest, ingen seek).
         */
        private long startOffset = 0;
    }
}
