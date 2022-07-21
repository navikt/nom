package no.nav.nom.apps.navident.eventsource;

import lombok.val;
import no.nav.nom.apps.navident.EmbeddedKafkaTestBase;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.rnrservices.Need;
import no.nav.nom.apps.navident.rnrservices.Respons;
import no.nav.nom.apps.navident.rnrservices.util.RnRTestListener;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Component
public class EventSourceProducerTest extends EmbeddedKafkaTestBase {

    @Autowired
    private Map<String, Navident> navidenter;

    @Value("${test.rnr-topic}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, Need> kafkaTemplate;

    @MockBean
    private SourceEventRepository sourceEventRepository;

    @Test
    public void opprettNavidentProdusererSourceEvent() {
        when(sourceEventRepository.save(any(SourceEvent.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);

        val need = Need.builder()
                .uuid(UUID.randomUUID().toString())
                .pdlOppslagVisited(true)
                .personOppslagVisited(true)
                .idGeneratorVisited(true)
                .responsProdusentVisited(true)
                .respons(Respons.builder().httpStatus(HttpStatus.CREATED).body(Respons.Body.builder().navident("A000000").build()).build())
                .build();
        kafkaTemplate.send(topic, need);

        Awaitility.await().atMost(15, SECONDS).pollInterval(10, MILLISECONDS).until(() -> navidenter.size() > 0);
    }
}
