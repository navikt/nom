package no.nav.nom.skjermede.personer.pip;

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@AllArgsConstructor
public class CacheTest {
    private final Cache cache = new Cache();

    @Test
    void nullTest() {
        Skjermingsperiode periode = new Skjermingsperiode(LocalDateTime.now(), null);
        assertDoesNotThrow(() -> {});
    }

}
