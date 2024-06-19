package no.nav.nom.apps.api.authorize.eventsourcing;

import no.nav.nom.apps.api.authorize.eventsourcing.producer.AuthorizeCommandService;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.AuthorizeEventKafkaConfig;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.AuthorizeEventProducer;
import no.nav.nom.apps.api.authorize.eventsourcing.recoverybackup.AuthorizeEventSourceRepository;
import no.nav.nom.apps.api.common.LocalConfiguration;
import no.nav.nom.libs.state.authorize.DelegertLederStateAutoconfiguration;
import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;

@Import({
    AuthorizeCommandService.class,
    AuthorizeEventProducer.class,
    AuthorizeEventKafkaConfig.class,
    NomStateDelegertLederListener.class,
    DelegertLederStateAutoconfiguration.class,
    KafkaProperties.class,
    LocalConfiguration.class,
})
@EnableKafka
@AutoConfigureJson
public class TestConfig {
    @MockBean
    AuthorizeEventSourceRepository authorizeEventSourceRepository;
}
