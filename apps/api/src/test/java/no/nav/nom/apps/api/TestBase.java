package no.nav.nom.apps.api;

import com.thomaskasene.wiremock.junit.WireMockStubs;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.libs.test.RsaStub;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ActiveProfiles({"test", "log-console-error"})
@WireMockStubs(RsaStub.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class TestBase {

    @Autowired
    private DvhRepository dvhRepository;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void reset() {
        dvhRepository.clearRepository();
        Mockito.reset(kafkaTemplate);
    }

    @SuppressWarnings("unchecked")
    protected List<ProducerRecord<String, String>> getRecordsFromKafka() {
        val captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate, atLeastOnce()).send(captor.capture());
        return captor.getAllValues().stream()
                .map(e -> (ProducerRecord<String, String>) e)
                .collect(toUnmodifiableList());
    }

}
