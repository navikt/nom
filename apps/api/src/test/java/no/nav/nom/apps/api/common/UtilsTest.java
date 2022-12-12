package no.nav.nom.apps.api.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void validateNavident() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Utils.validateNavident(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Utils.validateNavident(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Utils.validateNavident("a123456"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Utils.validateNavident("A1234567"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Utils.validateNavident("A123B56"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Utils.validateNavident("0123456"));

        Assertions.assertDoesNotThrow(() -> Utils.validateNavident("A123456"));
    }

}