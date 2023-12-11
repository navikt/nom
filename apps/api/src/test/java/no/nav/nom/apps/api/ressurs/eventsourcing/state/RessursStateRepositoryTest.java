package no.nav.nom.apps.api.ressurs.eventsourcing.state;

import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RessursStateRepositoryTest {

    RessursStateRepository ressursStateRepository = new RessursStateRepositoryImpl();

    @Test
    @DisplayName("Test remove RessursStateFor fid")
    void test_001() {
        RessursState stateToDelete = new RessursState("FID_1", "123", "A000001", Sektor.NAV_STATLIG, null, null, null, null, Collections.emptyList());

        ressursStateRepository.putEventAggregate(stateToDelete);

        assertEquals(stateToDelete, ressursStateRepository.findByFid("FID_1").get());
        assertEquals(stateToDelete, ressursStateRepository.findByPersonident("123").get());
        assertEquals(stateToDelete, ressursStateRepository.findByNavident("A000001").get());

        ressursStateRepository.remove("FID_1");

        assertTrue(ressursStateRepository.findByFid("FID_1").isEmpty());
        assertTrue(ressursStateRepository.findByPersonident("123").isEmpty());
        assertTrue(ressursStateRepository.findByNavident("A000001").isEmpty());
    }

    @Test
    @DisplayName("Test remove RessursStateFor fid hvor det er to ulike fid for samme navident, forskjellig personident")
    void test_002() {
        RessursState originalState = new RessursState("FID_1", "123", "A000001", Sektor.NAV_STATLIG, null, null, null, null, Collections.emptyList());
        RessursState duplicateStateToDelete = new RessursState("FID_2", "321", "A000001", Sektor.NAV_STATLIG, null, null, null, null, Collections.emptyList());

        ressursStateRepository.putEventAggregate(originalState);
        ressursStateRepository.putEventAggregate(duplicateStateToDelete); // This takes over as RessursByNavident state

        assertEquals(originalState, ressursStateRepository.findByFid("FID_1").get());
        assertEquals(originalState, ressursStateRepository.findByPersonident("123").get());
        assertNotEquals(originalState, ressursStateRepository.findByNavident("A000001").get());

        assertEquals(duplicateStateToDelete, ressursStateRepository.findByFid("FID_2").get());
        assertEquals(duplicateStateToDelete, ressursStateRepository.findByPersonident("321").get());
        assertEquals(duplicateStateToDelete, ressursStateRepository.findByNavident("A000001").get());

        ressursStateRepository.remove("FID_2");

        assertTrue(ressursStateRepository.findByFid("FID_2").isEmpty());
        assertTrue(ressursStateRepository.findByPersonident("123").isPresent());
        assertTrue(ressursStateRepository.findByPersonident("321").isEmpty());
        assertTrue(ressursStateRepository.findByNavident("A000001").isPresent());
        assertEquals("FID_1", ressursStateRepository.findByNavident("A000001").get().fid());
        assertEquals("FID_1", ressursStateRepository.findByPersonident("123").get().fid());
    }

    @Test
    @DisplayName("Test remove RessursStateFor fid hvor det er to ulike fid for samme navident og samme personident")
    void test_003() {
        RessursState originalState = new RessursState("FID_1", "123", "A000001", Sektor.NAV_STATLIG, null, null, null, null, Collections.emptyList());
        RessursState duplicateStateToDelete = new RessursState("FID_2", "123", "A000001", Sektor.NAV_STATLIG, null, null, null, null, Collections.emptyList());

        ressursStateRepository.putEventAggregate(originalState);
        ressursStateRepository.putEventAggregate(duplicateStateToDelete); // This takes over as RessursByNavident state

        assertEquals(originalState, ressursStateRepository.findByFid("FID_1").get());
        assertNotEquals(originalState, ressursStateRepository.findByPersonident("123").get());
        assertNotEquals(originalState, ressursStateRepository.findByNavident("A000001").get());

        assertEquals(duplicateStateToDelete, ressursStateRepository.findByFid("FID_2").get());
        assertEquals(duplicateStateToDelete, ressursStateRepository.findByPersonident("123").get());
        assertEquals(duplicateStateToDelete, ressursStateRepository.findByNavident("A000001").get());

        ressursStateRepository.remove("FID_2");

        assertTrue(ressursStateRepository.findByFid("FID_2").isEmpty());
        assertTrue(ressursStateRepository.findByPersonident("123").isPresent());
        assertTrue(ressursStateRepository.findByNavident("A000001").isPresent());
        assertEquals("FID_1", ressursStateRepository.findByNavident("A000001").get().fid());
        assertEquals("FID_1", ressursStateRepository.findByPersonident("123").get().fid());
    }
}