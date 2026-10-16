package no.nav.nom.apps.sfintegrasjon.organisasjon.eventsource;

import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelse;
import no.nav.nom.apps.sfintegrasjon.organisasjon.repository.OrganisasjonHendelseRepository;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(EvtsrcOffsetRestartTest.Config.class)
@TestPropertySource(properties = {
    "app.kafka.evtsrc.group-id=nom-sf-integrasjon-evtsrc-test",
    "app.kafka.evtsrc.retry-delay-ms=10",
    "app.kafka.evtsrc.orgenhet.topic=org.nom.orgenhet-eventsource",
    "app.kafka.evtsrc.orgenhet-leder.topic=org.nom.orgenhet-leder-eventsource",
    "app.kafka.evtsrc.organisering.topic=org.nom.organisering-eventsource"
})
@EmbeddedKafka(partitions = 1, topics = {"org.nom.orgenhet-eventsource", "org.nom.orgenhet-leder-eventsource", "org.nom.organisering-eventsource"})
@DirtiesContext
class EvtsrcOffsetRestartTest {
    private static final String TOPIC = "org.nom.orgenhet-eventsource";
    private static final TopicPartition PARTITION = new TopicPartition(TOPIC, 0);

    @Autowired EmbeddedKafkaBroker broker;
    @Autowired KafkaListenerEndpointRegistry registry;
    @Autowired AdminClient admin;
    @Autowired OrgEnhetStateRepository states;
    @Autowired OrganisasjonHendelseRepository repository;

    @Test
    void failedRecordIsNotCommittedAndIsDeliveredAgainAfterRestart() throws Exception {
        var stateAvailable = new AtomicBoolean(false);
        when(states.findByFid("valid-fid"))
            .thenReturn(Optional.of(OrgEnhetState.builder().fid("valid-fid").id("123456").build()));
        when(states.findByFid("failed-fid")).thenAnswer(_ -> stateAvailable.get()
            ? Optional.of(OrgEnhetState.builder().fid("failed-fid").id("123456").build())
            : Optional.empty());

        try (var producer = new KafkaProducer<String, String>(Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            assertThat(producer.send(new ProducerRecord<>(TOPIC, "first", "{\"fid\":\"valid-fid\",\"type\":\"Created\"}"))
                .get(10, TimeUnit.SECONDS).offset()).isZero();
            assertThat(producer.send(new ProducerRecord<>(TOPIC, "second", "{\"fid\":\"failed-fid\",\"type\":\"Changed\"}"))
                .get(10, TimeUnit.SECONDS).offset()).isEqualTo(1L);
        }

        // Testen måler ack ved feil, ikke initial auto.offset.reset=latest eller seek ved første oppstart.
        admin.alterConsumerGroupOffsets(EvtsrcKafkaListener.ORGENHET_LISTENER_ID,
            Map.of(PARTITION, new OffsetAndMetadata(0L))).all().get(10, TimeUnit.SECONDS);
        assertThat(committedOffset()).isZero();

        MessageListenerContainer orgenhet = registry.getListenerContainer(EvtsrcKafkaListener.ORGENHET_LISTENER_ID);
        MessageListenerContainer leder = registry.getListenerContainer(EvtsrcKafkaListener.ORGENHET_LEDER_LISTENER_ID);
        MessageListenerContainer organisering = registry.getListenerContainer(EvtsrcKafkaListener.ORGANISERING_LISTENER_ID);
        assertThat(orgenhet).isNotNull();
        assertThat(leder).isNotNull();
        assertThat(organisering).isNotNull();

        orgenhet.start();
        leder.start();
        organisering.start();
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(orgenhet.isRunning()).isFalse();
            assertThat(leder.isRunning()).isFalse();
            assertThat(organisering.isRunning()).isFalse();
            assertThat(committedOffset()).isEqualTo(1L);
            verify(repository, times(1)).save(any(OrganisasjonHendelse.class));
        });

        stateAvailable.set(true);
        orgenhet.start();
        try {
            Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                assertThat(committedOffset()).isEqualTo(2L);
                var saved = ArgumentCaptor.forClass(OrganisasjonHendelse.class);
                verify(repository, times(2)).save(saved.capture());
                assertThat(saved.getAllValues()).extracting(OrganisasjonHendelse::getFid)
                    .containsExactly("valid-fid", "failed-fid");
            });
        } finally {
            orgenhet.stop();
        }
    }

    private long committedOffset() throws Exception {
        Map<TopicPartition, OffsetAndMetadata> offsets = admin
            .listConsumerGroupOffsets(EvtsrcKafkaListener.ORGENHET_LISTENER_ID)
            .partitionsToOffsetAndMetadata().get(10, TimeUnit.SECONDS);
        return Optional.ofNullable(offsets.get(PARTITION)).map(OffsetAndMetadata::offset).orElse(-1L);
    }

    @Configuration
    @EnableKafka
    @Import(EvtsrcKafkaConfig.class)
    static class Config {
        @Bean KafkaProperties kafkaProperties(EmbeddedKafkaBroker broker) {
            var properties = new KafkaProperties();
            properties.setBootstrapServers(List.of(broker.getBrokersAsString()));
            return properties;
        }

        @Bean EvtsrcKafkaListener evtsrcKafkaListener(OrganisasjonHendelseRepository repository,
                                                      EvtsrcKafkaProperties properties, AdminClient admin,
                                                      OrgEnhetStateRepository orgEnhet,
                                                      OrgEnhetLederStateRepository leder,
                                                      OrganiseringStateRepository organisering) {
            return new EvtsrcKafkaListener(repository, JsonMapper.builder().build(), properties, admin,
                orgEnhet, leder, organisering);
        }

        @Bean OrgEnhetStateRepository orgEnhetStateRepository() { return mock(OrgEnhetStateRepository.class); }
        @Bean OrgEnhetLederStateRepository orgEnhetLederStateRepository() { return mock(OrgEnhetLederStateRepository.class); }
        @Bean OrganiseringStateRepository organiseringStateRepository() { return mock(OrganiseringStateRepository.class); }
        @Bean OrganisasjonHendelseRepository organisasjonHendelseRepository() { return mock(OrganisasjonHendelseRepository.class); }
    }
}
