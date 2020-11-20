package no.nav.nom.apps.dvh.enhet;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import lombok.val;
import no.nav.nom.contract.dvh.Enhet;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("Enhet")
class EnhetDvhTest extends EnhetTestBase {

    @Test
    @DisplayName("Sender data fra DVH til Kafka og oppdaterer offset etter transaksjon mot Kafka er ferdig")
    void scenario11() {
        doReturn(Optional.of(1337L)).when(stateRepository).findSequenceFor(anyString());

        task.perform();

        val inOrder = inOrder(kafkaTemplate, stateRepository);
        inOrder.verify(kafkaTemplate).executeInTransaction(any());
        inOrder.verify(stateRepository).saveOffset(Enhet.TYPE, 1338L);

        val sent = getRecordsFromKafka();
        val first = sent.get(0);
        assertBasicRecord(first, properties.getDvh().getTopic(), "Enhet-1337");
        assertBasicDvhEvent(first, Enhet.TYPE);
        assertTrue(first.value().contains("\"ORGNODE_KODE\":\"000000\","));

        val second = sent.get(1);
        assertBasicRecord(second, properties.getDvh().getTopic(), "Enhet-1338");
        assertBasicDvhEvent(first, Enhet.TYPE);
        assertTrue(second.value().contains("\"ORGNODE_KODE\":\"000\","));
    }

    @Test
    @DisplayName("Kaller DVH med offset 0 hvis ingen offset finnes")
    void scenario12() {
        doReturn(Optional.empty()).when(stateRepository).findSequenceFor(anyString());

        task.perform();

        WireMock.verify(getRequestedFor(urlEqualTo("/json/Hr/Nom/NOM_HRORG_HIEARKI_PARAM?in_sekvens_fra=0")));
    }

    @Test
    @DisplayName("Tom response støttes, oppdaterer ikke state og oppretter ikke transaksjon mot Kafka")
    void scenario14() {
        doReturn(Optional.of(0L)).when(stateRepository).findSequenceFor(anyString()); // Offset 0 har tom liste.

        task.perform();

        verify(kafkaTemplate, never()).executeInTransaction(any());
        verify(stateRepository, never()).saveOffset(any(), anyLong());
    }

    @Test
    @DisplayName("Gitt ikke sortert rekkefølge på enhet så skal vi lagre unna max offset")
    void scenario21() {
        doReturn(Optional.of(1337L)).when(stateRepository).findSequenceFor(anyString());

        task.perform();

        verify(stateRepository).saveOffset(Enhet.TYPE, 1338L);
    }

    @Test
    @DisplayName("DVH ClientException, ingen kafka eller state endringer")
    void scenario31() {
        doReturn(Optional.empty()).when(stateRepository).findSequenceFor(anyString());
        stubFor(get(UrlPattern.ANY).willReturn(badRequest()));

        task.perform();

        verify(kafkaTemplate, never()).executeInTransaction(any());
        verify(stateRepository, never()).saveOffset(any(), anyLong());
    }

    @Test
    @DisplayName("DVH ServerException, ingen kafka eller state endringer")
    void scenario32() {
        doReturn(Optional.empty()).when(stateRepository).findSequenceFor(anyString());
        stubFor(get(UrlPattern.ANY).willReturn(serverError()));

        task.perform();

        verify(kafkaTemplate, never()).executeInTransaction(any());
        verify(stateRepository, never()).saveOffset(any(), anyLong());
    }
}
