package no.nav.nom.apps.dvh.stilling;

import lombok.val;
import no.nav.nom.contract.dvh.Stilling;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Stilling")
public class StillingDvhTest extends StillingTestBase {

    @Test
    @DisplayName("Sender data fra DVH til Kafka og sjekker at ny data ligger på køen")
    void scenario11() {
        stillingDvhTask.perform();

        val sent = getRecordsFromKafka();
        val stillingRecord = sent.get(0);
        assertBasicRecord(stillingRecord, properties.getDvh().getTopic(), "StillingTabell");
        assertBasicDvhEvent(stillingRecord, Stilling.TYPE);
        assertTrue(stillingRecord.value().contains("\"RESOURCE_ID\":\"146783\""));
        assertTrue(stillingRecord.value().contains("\"RESOURCE_ID\":\"132400\""));
        assertTrue(stillingRecord.value().contains("\"RESOURCE_ID\":\"109915\""));
    }
}
