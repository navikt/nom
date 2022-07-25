package no.nav.nom.apps.navident.rnrservices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.common.cache.NeedClientTokenCache;
import no.nav.nom.apps.navident.domain.IdentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NeedProducer {
    final KafkaTemplate<String, Need> kafkaTemplate;
    final NeedClientTokenCache cache;

    @Value("${app.kafka.topic.rnr.name}")  private String topic;


    public String produceNeed(char idPrefix, String personid, IdentType type) {
        return produceNeed(idPrefix, personid, type, LocalDate.now());
    }

    public String produceNeed(char idPrefix, String personid, IdentType type, LocalDate fraDato) {
        String uuid = UUID.randomUUID().toString();

        // Want to be able to use this token in oauth2 obo token generation for pdl lookup in async rnr flow
        cache.putOauth2TokenFromSession(uuid);

        Need need = Need.builder()
                .uuid(uuid)
                .idPrefix(idPrefix)
                .personid(personid)
                .identType(type)
                .build();
        kafkaTemplate.send(topic, need);

        log.debug("Publisert Need melding: %s".formatted(need.maskedToString()));
        return uuid;
    }



}
