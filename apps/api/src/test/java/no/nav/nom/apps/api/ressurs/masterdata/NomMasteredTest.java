package no.nav.nom.apps.api.ressurs.masterdata;

import no.nav.nom.apps.api.TestBase;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class NomMasteredTest extends TestBase {
    @Autowired
    NomMastered nomMastered;

    @MockBean
    RessursStateRepository ressursStateRepository;

    @Test
    @DisplayName("MarkRessursAsMastered for unknown ressurs")
    void test_001() {
        assertFalse(nomMastered.markRessursAsMasteredInNom("UNKNOWN"));

        assertEquals(0, nomMastered.count());
    }

    @Test
    @DisplayName("MarkRessursAsMastered for ressurs")
    void test_002() {
        assertTrue(markRessursAsMastered(Ressurs_A000001.navident()));

        assertEquals(1, nomMastered.count());
        assertTrue(nomMastered.isMastered(Ressurs_A000001.navident()));
    }

    @Test
    @DisplayName("MarkRessursAsMastered then unmark")
    void test_010() {
        assertTrue(markRessursAsMastered(Ressurs_A000001.navident()));

        assertEquals(1, nomMastered.count());
        assertTrue(nomMastered.isMastered(Ressurs_A000001.navident()));

        assertTrue(unmarkRessursAsMastered(Ressurs_A000001.navident()));

        assertEquals(0, nomMastered.count());
        assertFalse(nomMastered.isMastered(Ressurs_A000001.navident()));
    }

    @BeforeEach
    void setUp() {
        nomMastered.clear();
        when(ressursStateRepository.findByNavident(Ressurs_A000001.navident())).thenReturn(Optional.of(Ressurs_A000001));
    }

    private static final RessursState Ressurs_A000001 =
        RessursState.builder()
            .navident("A0000001")
            .fid("ressurs-A1")
            .build();


    public synchronized boolean markRessursAsMastered(String navident) {
        CountDownLatch latch = new CountDownLatch(1);

        Mockito.doAnswer(invocation -> {
            invocation.callRealMethod();
            latch.countDown();
            return null;
        }).when(kafkaUtils.nomMasteredRessursListener).onMessage(Mockito.any(), Mockito.any());


        boolean result = nomMastered.markRessursAsMasteredInNom(navident);
        try {
            if (!latch.await(3, TimeUnit.SECONDS)) {
                throw new RuntimeException("Invocation of kafka onMessage never occoured or never exited");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public synchronized boolean unmarkRessursAsMastered(String ressursFid) {
        CountDownLatch latch = new CountDownLatch(1);

        Mockito.doAnswer(invocation -> {
            invocation.callRealMethod();
            latch.countDown();
            return null;
        }).when(kafkaUtils.nomMasteredRessursListener).onMessage(Mockito.any(), Mockito.any());


        boolean result = nomMastered.unmarkRessursAsMasteredInNom(ressursFid);
        try {
            if (!latch.await(3, TimeUnit.SECONDS)) {
                throw new RuntimeException("Invocation of kafka onMessage never occoured or never exited");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}