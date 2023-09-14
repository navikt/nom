package no.nav.nom.apps.nom.unit4.kafka.utils;

import no.nav.nom.apps.nom.unit4.AppProperties;
import no.nav.nom.apps.nom.unit4.kafka.KafkaRessursListener;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaUtils {
    @SpyBean
    private KafkaRessursListener kafkaRessursListener;
    @Autowired
    private KafkaTemplate<String, RessursEvent> kafkaTemplate;

    @Autowired
    private AppProperties appProperties;


    public synchronized void send(String key, RessursEvent event){
        CountDownLatch latch = new CountDownLatch(1);

        Mockito.doAnswer(invocation -> {
            invocation.callRealMethod();
            latch.countDown();
            return null;
        }).when(kafkaRessursListener).listener(Mockito.any(), Mockito.any());

        kafkaTemplate.send(appProperties.ressurs().topic(), key, event);
        try {
            if (!latch.await(4, TimeUnit.SECONDS)){ // økt til 4 sec - av en eller annen grunn fikk jeg plutselig sporadiske timeouts på mvn verify. /Roy
                throw new RuntimeException("Invocation of kafka onMessage never occurred or never exited");
            }
        } catch (InterruptedException e){
            throw new RuntimeException(e);
        }
    }

}
