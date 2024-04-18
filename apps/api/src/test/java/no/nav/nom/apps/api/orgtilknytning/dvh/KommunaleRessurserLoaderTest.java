package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Tester last og parsing av kommunale ressurser fra DVH")
class KommunaleRessurserLoaderTest  extends TestBase {
    @Autowired
    private KommunaleRessurserLoader kommunaleRessurserLoader;
    @MockBean
    OrgenhetService orgenhetService;
    @MockBean
    RessursStateRepository ressursStateRepository;

    @Test
    @DisplayName("Test last fra Dvh json")
    public void testLoad() {

        var result = kommunaleRessurserLoader.loadKommunaleForNavidenter(List.of("N123321","G123321"));;

        assertEquals(2, result.size());
        var kari = result.stream().filter(r -> r.getNavIdent().equals("N123321")).findFirst().get();
        assertEquals("N123321", kari.getNavIdent());
        assertEquals("Kari", kari.getFornavn());
        assertNull(kari.getAgressoStartDato());
        assertEquals(LocalDate.of(2022, 1, 10), kari.getStartDato());
        assertEquals(LocalDate.of(2022, 1, 10), kari.getRemedyStartDato());
    }
}