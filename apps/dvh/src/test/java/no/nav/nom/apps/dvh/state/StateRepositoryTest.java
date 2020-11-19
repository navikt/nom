package no.nav.nom.apps.dvh.state;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("StateRepository")
class StateRepositoryTest extends StateTestBase {

    @Test
    @DisplayName("StateRepository returnerer state fra StateLoader")
    void scenario11() {
        doReturn(Map.of("key", 10L)).when(loader).loadState();

        val result = repository.findSequenceFor("key");

        assertEquals(10L, result.get());
    }

    @Test
    @DisplayName("StateRepository returnerer empty fra StateLoader når state ikke finnes")
    void scenario12() {
        doReturn(emptyMap()).when(loader).loadState();

        val result = repository.findSequenceFor("key");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("StateRepository kaller StateLoader kun en gang")
    void scenario13() {
        doReturn(emptyMap()).when(loader).loadState();

        repository.findSequenceFor("1");
        repository.findSequenceFor("2");
        repository.findSequenceFor("3");

        verify(loader, times(1)).loadState();
    }

    @Test
    @DisplayName("StateRepository returnerer oppdatert state")
    void scenario14() {
        doReturn(Map.of("key", 10L)).when(loader).loadState();
        repository.findSequenceFor("key"); // Trigger initiell load fra StateLoader.
        repository.saveOffset("key", 11L);

        val result = repository.findSequenceFor("key");

        assertEquals(11L, result.get());
    }


}
