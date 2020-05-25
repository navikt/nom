package no.nav.nom.skjermede.personer.pip;

import lombok.val;
import no.nav.nom.skjermede.personer.pip.Cache;
import no.nav.nom.skjermede.personer.pip.Skjermingsperiode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IsSkjermetTest {

    private final Cache cache = new Cache();

    @Test
    void returnsTrueWhenWithinPeriod() {
        val fra = LocalDateTime.of(2020, 1, 1, 13, 00);
        val til = LocalDateTime.now().plusMonths(6);
        cache.store("1", new Skjermingsperiode(fra, til));

        assertTrue(cache.isSkjermet("1"));
    }

    @Test
    void returnsTrueWhenPeriodToIsNull() {
        val fra = LocalDateTime.of(2020, 1, 1, 13, 00);
        cache.store("2", new Skjermingsperiode(fra));

        assertTrue(cache.isSkjermet("2"));
    }

    @Test
    void returnsFalseWhenPeriodHasPassed() {
        val fra = LocalDateTime.of(2020, 1, 1, 13, 00);
        val til = LocalDateTime.now().minusMonths(6);
        cache.store("3", new Skjermingsperiode(fra, til));

        assertFalse(cache.isSkjermet("3"));
    }

    @Test
    void returnsFalseWhenSkjermingDoesNotExist() {
        assertFalse(cache.isSkjermet("NonExistingPersonident"));
    }
}
