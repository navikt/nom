package no.nav.nom.skjermede.personer.pip;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.skjermede.dto.SkjermetPersonDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = ApplicationStarter.class,
        properties = { "spring.kafka.consumer.client-id=ITSkjermedePersonerListenerTest" })
@ActiveProfiles("local")
@Slf4j
public class ITSkjermedePersonerListenerTest {

    @Autowired
    private Cache cache;

    @Autowired
    private KafkaTestProducerService kafkaProducer;

    @Autowired
    private CacheCallbackAspect cacheCallback;

    @Test
    void read_message_from_kafka_topic() throws Exception {
        cache.getAll().clear(); // We may have hooked up to none-empty KafkaTopic
        assert(!cache.isSkjermet("03019012345"));

        CountDownLatch latch = new CountDownLatch(1);

        cacheCallback.addCallback(new Callback() {
            @Override
            public void notifyMe() {
                latch.countDown();
            }
        });

        kafkaProducer.send("03019012345",
                SkjermetPersonDTO.builder()
                        .skjermetFra(LocalDateTime.now().minusYears(1))
                        .build());

        latch.await(1, TimeUnit.SECONDS);

        assert(cache.size() == 1);
        assert(cache.isSkjermet("03019012345"));
    }
}
