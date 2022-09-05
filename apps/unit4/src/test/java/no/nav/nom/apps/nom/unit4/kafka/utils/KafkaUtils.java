package no.nav.nom.apps.nom.unit4.kafka.utils;

import no.nav.nom.apps.nom.unit4.kafka.KafkaRessursListener;
import no.nav.nom.apps.nom.unit4.kafka.NomRessursDto;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaUtils {
    @Value("${app.ressurs.topic}") String ressursTopic;

    @SpyBean
    private KafkaRessursListener kafkaRessursListener;
    @Autowired
    private KafkaTemplate<String, NomRessursDto> ressursKafkaTemplate;


    public synchronized void send(String key, NomRessursDto data){
        CountDownLatch latch = new CountDownLatch(1);

        Mockito.doAnswer(invocation -> {
            invocation.callRealMethod();
            latch.countDown();
            return null;
        }).when(kafkaRessursListener).listener(Mockito.any(), Mockito.any());

        ressursKafkaTemplate.send(ressursTopic, key, data);
        try {
            if (!latch.await(2, TimeUnit.SECONDS)){
                throw new RuntimeException("Invocation of kafka onMessage never occoured or never exited");
            }
        } catch (InterruptedException e){
            throw new RuntimeException(e);
        }
    }

}
