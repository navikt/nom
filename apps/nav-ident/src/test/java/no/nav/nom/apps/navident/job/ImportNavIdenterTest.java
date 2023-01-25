package no.nav.nom.apps.navident.job;

import no.nav.nom.apps.navident.SpringBootTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class ImportNavIdenterTest extends SpringBootTestBase {

    @Test()
    @DisplayName("Importer single linje - ident som ikke finnes")
    void importLineNewOk() {
        String line = "F123456, ";
    }
}