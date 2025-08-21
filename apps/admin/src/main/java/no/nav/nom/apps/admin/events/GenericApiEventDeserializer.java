package no.nav.nom.apps.admin.events;

import no.nav.nom.apps.admin.dto.hendelse.HendelseDTO;
import no.nav.nom.apps.admin.dto.orgenhet.OrgEnhetStateDTO;
import no.nav.nom.apps.admin.dto.ressurs.RessursStateKafkaDTO;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;

public class GenericApiEventDeserializer implements Deserializer<Object> {
    private final Logger logger = LoggerFactory.getLogger(GenericApiEventDeserializer.class);
    private final Map<String, JsonSerde<?>> serdeMappings;

    public GenericApiEventDeserializer() {
        this.serdeMappings = new HashMap<>();
        serdeMappings.put("no.nav.nom.apps.streams.kafkaapi.ressurs.state.v2.RessursState", new JsonSerde<>(RessursStateKafkaDTO.class));
        serdeMappings.put("no.nav.nom.apps.streams.kafkaapi.ressurs.state.v3.RessursState", new JsonSerde<>(RessursStateKafkaDTO.class));
        serdeMappings.put("no.nav.nom.apps.streams.kafkaapi.orgenhet.state.v3.OrgEnhetState", new JsonSerde<>(OrgEnhetStateDTO.class));
        serdeMappings.put("no.nav.nom.apps.streams.kafkaapi.hendelser.v1.Hendelse", new JsonSerde<>(HendelseDTO.class));
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        serdeMappings.values().forEach(serde -> serde.configure(configs, isKey));
    }

    @Override
    public Object deserialize(String topic, byte[] data) {
        throw new UnsupportedOperationException("This method should not be called without headers");
    }

    @Override
    public Object deserialize(String topic, Headers headers, byte[] data) {
        if (headers.lastHeader("__TypeId__") != null) {
            String typeIdValue = new String(headers.lastHeader("__TypeId__").value());
            JsonSerde<?> targetSerde = serdeMappings.get(typeIdValue);

            if (!isNull(targetSerde)) {
                try {
                    return targetSerde.deserializer().deserialize(topic, data);
                } catch (Exception e) {
                    logger.error("Failed to deserialize RessursStateKafkaDTO", e);
                }
            } else {
                logger.warn("Unsupported type: {}", typeIdValue);
            }
        }
        logger.warn("No __TypeId__ header found. Headers: {}", headers);
        return null;
    }
}
