package no.nav.nom.apps.dvh.ressurs;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import lombok.val;
import no.nav.nom.contract.dvh.Ressurs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@DisplayName("Ressurs")
class RessursDvhTest extends RessursTestBase {

    @Test
    @DisplayName("Sender data fra DVH til Kafka og oppdaterer offset etter hver enkelt chunk fra DVH")
    void scenario11() {

        doAnswer(new Answer<Optional<Long>>() {
            private int count = 0;

            @Override
            public Optional<Long> answer(InvocationOnMock invocationOnMock) {
                return Optional.of((++count) == 1 ? 2L : 5L);
            }
        }).when(stateRepository).findSequenceFor(anyString());

        ressursDvhTask.perform();

        val inOrder = inOrder(kafkaTemplate, stateRepository);
        inOrder.verify(kafkaTemplate).executeInTransaction(any());
        inOrder.verify(stateRepository).saveOffset(Ressurs.TYPE, 5L);
        inOrder.verify(kafkaTemplate).executeInTransaction(any());
        inOrder.verify(stateRepository).saveOffset(Ressurs.TYPE, 6L);



        val sent = getRecordsFromKafka();
        val first = sent.get(0);
        assertBasicRecord(first, properties.getDvh().getTopic(), "Ressurs-3");
        assertTrue(first.value().contains("\"NAV_IDENT\":\"Z111113\","));

        val second = sent.get(1);
        assertBasicRecord(second, properties.getDvh().getTopic(), "Ressurs-4");
        assertTrue(second.value().contains("\"NAV_IDENT\":\"Z111114\","));
    }

    @Test
    @DisplayName("Kaller DVH med offset 0 til chunkSize hvis ingen offset finnes")
    void scenario13() {
        doReturn(Optional.empty()).when(stateRepository).findSequenceFor(anyString());

        ressursDvhTask.perform();

        WireMock.verify(getRequestedFor(urlEqualTo("/json/Hr/Nom/Nom_Ansatte?in_sekvensnummer_fra=0&in_sekvensnummer_til="+properties.getDvh().getRessurs().getChunkSize())));
    }

    @Test
    @DisplayName("Tom response støttes, oppdaterer ikke state og oppretter ikke transaksjon mot Kafka")
    void scenario14() {
        doReturn(Optional.of(8L)).when(stateRepository).findSequenceFor(anyString()); // Offset 0 har tom liste.

        ressursDvhTask.perform();

        verify(kafkaTemplate, never()).executeInTransaction(any());
        verify(stateRepository, never()).saveOffset(any(), anyLong());
    }

    @Test
    @DisplayName("Gitt ikke sortert rekkefølge på enhet så skal vi lagre unna max offset")
    void scenario21() {
        doReturn(Optional.of(0L)).when(stateRepository).findSequenceFor(anyString());

        ressursDvhTask.perform();

        verify(stateRepository).saveOffset(Ressurs.TYPE, 2L);
    }

    @Test
    @DisplayName("DVH ClientException, ingen kafka eller state endringer")
    void scenario31() {
        doReturn(Optional.empty()).when(stateRepository).findSequenceFor(anyString());
        stubFor(get(UrlPattern.ANY).willReturn(badRequest()));

        ressursDvhTask.perform();

        verify(kafkaTemplate, never()).executeInTransaction(any());
        verify(stateRepository, never()).saveOffset(any(), anyLong());
    }

    @Test
    @DisplayName("DVH ServerException, ingen kafka eller state endringer")
    void scenario32() {
        doReturn(Optional.empty()).when(stateRepository).findSequenceFor(anyString());
        stubFor(get(UrlPattern.ANY).willReturn(serverError()));

        ressursDvhTask.perform();

        verify(kafkaTemplate, never()).executeInTransaction(any());
        verify(stateRepository, never()).saveOffset(any(), anyLong());
    }
}
