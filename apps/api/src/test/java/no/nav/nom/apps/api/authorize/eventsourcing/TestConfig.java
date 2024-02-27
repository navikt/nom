package no.nav.nom.apps.api.authorize.eventsourcing;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.AuthorizeCommandService;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.AuthorizeEventKafkaConfig;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.AuthorizeEventProducer;
import no.nav.nom.apps.api.authorize.eventsourcing.recoverybackup.AuthorizeEventSourceRepository;
import no.nav.nom.apps.api.authorize.eventsourcing.state.DelegertLederEventListener;
import no.nav.nom.apps.api.authorize.eventsourcing.state.DelegertLederKafkaListenerConfig;
import no.nav.nom.apps.api.authorize.eventsourcing.state.DelegertLederStateRepository;
import no.nav.nom.apps.api.common.LocalConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;

@Import({
    AuthorizeCommandService.class,
    AuthorizeEventProducer.class,
    AuthorizeEventKafkaConfig.class,
    DelegertLederKafkaListenerConfig.class,
    DelegertLederEventListener.class,
    DelegertLederStateRepository.class,
    KafkaProperties.class,
    LocalConfiguration.class,
    ObjectMapper.class
})
@EnableKafka
public class TestConfig {
    @MockBean
    AuthorizeEventSourceRepository authorizeEventSourceRepository;
}
