package no.nav.nom.apps.navident.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NavidentControllerTest {

    NavidentController navidentController = new NavidentController(null, null);

    @Test
    @DisplayName("Test konvertering av identprefiks med aksent/diacritic og nasjonale bokstaver til A-Z")
    void test_001() {
        assertEquals('A', navidentController.prefixValidator("A"));
        assertEquals('B', navidentController.prefixValidator("b"));
        assertEquals('C', navidentController.prefixValidator("C"));
        assertEquals('Z', navidentController.prefixValidator("z"));


        assertEquals('A', navidentController.prefixValidator("Æ"));
        assertEquals('A', navidentController.prefixValidator("æ"));
        assertEquals('O', navidentController.prefixValidator("Ø"));
        assertEquals('O', navidentController.prefixValidator("ø"));
        assertEquals('A', navidentController.prefixValidator("Å"));
        assertEquals('A', navidentController.prefixValidator("å"));

        assertEquals('A', navidentController.prefixValidator("Ä"));
        assertEquals('O', navidentController.prefixValidator("Ö"));

        assertEquals('E', navidentController.prefixValidator("É"));
        assertEquals('A', navidentController.prefixValidator("ă"));
        assertEquals('O', navidentController.prefixValidator("ő"));
    }
}