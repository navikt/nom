package no.nav.nom.libs.utils.common.kafka;

import java.util.UUID;

public class KafkaUtils {

    public static void assignRandomTransactionIdPrefix() {
        System.setProperty("kafka-transaction-id-prefix", UUID.randomUUID().toString().substring(0, 8));
    }
}
