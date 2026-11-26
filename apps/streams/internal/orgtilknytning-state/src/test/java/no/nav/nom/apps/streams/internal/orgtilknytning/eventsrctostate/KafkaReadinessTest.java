package no.nav.nom.apps.streams.internal.orgtilknytning.eventsrctostate;

import org.apache.kafka.streams.KafkaStreams;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaReadinessTest {

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean = mock(StreamsBuilderFactoryBean.class);
    private final KafkaReadiness kafkaReadiness = new KafkaReadiness(streamsBuilderFactoryBean);

    @Test
    void reportsDownBeforeKafkaStreamsExists() {
        when(streamsBuilderFactoryBean.getKafkaStreams()).thenReturn(null);

        assertThat(kafkaReadiness.health().getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void reportsOutOfServiceWhenKafkaStreamsIsInError() {
        var kafkaStreams = mock(KafkaStreams.class);
        when(streamsBuilderFactoryBean.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.state()).thenReturn(KafkaStreams.State.ERROR);

        assertThat(kafkaReadiness.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
    }
}
