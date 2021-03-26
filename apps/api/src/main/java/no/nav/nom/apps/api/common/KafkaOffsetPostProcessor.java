package no.nav.nom.apps.api.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.utils.common.kafka.KafkaUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.kafka.core.ConsumerPostProcessor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOffsetPostProcessor implements ConsumerPostProcessor<Object, Object> {

    @Getter
    private AtomicLong hendelseOffset = new AtomicLong(-1);
    private final AppProperties appProperties;

    @Override
    public Consumer<Object, Object> apply(Consumer<Object, Object> consumer) {
        consumer.listTopics()
                .forEach((key, value) -> {
                    if (key.equals(appProperties.getHendelse().getTopic())) {
                        hendelseOffset.set(KafkaUtils.findEndOffsetForPartition(consumer, appProperties.getHendelse().getTopic(), 0));
                        log.info("Hendelse offset {}", hendelseOffset.get());
                    }
                });
        return consumer;
    }
}
