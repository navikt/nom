package no.nav.nom.apps.navident.rnrservices.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.common.cache.RnRMdcCache;
import no.nav.nom.apps.navident.rnrservices.Need;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RnRTestNeedProducer {
    final KafkaTemplate<String, Need> kafkaTemplate;
    final RnRMdcCache mdcCache;

    @Value("${test.rnr-topic}")
    private String topic;

    public String publish(Need need) {
        String uuid = UUID.randomUUID().toString();
        need.setUuid(uuid);
        mdcCache.cacheMdc(uuid);
        kafkaTemplate.send(topic, need);
        log.debug("Publisert Need melding: %s".formatted(need.maskedToString()));
        return uuid;
    }
}
