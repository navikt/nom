package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Tester last og parsing av kommunale ressurser fra DVH")
class KommunaleRessurserLoaderTest  extends TestBase {
    @Autowired
    private KommunaleRessurserLoader kommunaleRessurserLoader;

    @Test
    @DisplayName("Test last fra Dvh json")
    public void testLoad() {

        var result = kommunaleRessurserLoader.loadKommunaleForNavidenter(List.of("A321003","A321004"));;

        assertEquals(2, result.size());
        var A321003 = result.stream().filter(r -> r.getNavIdent().equals("A321003")).findFirst().get();
        assertEquals("A321003", A321003.getNavIdent());
        assertEquals("Navn 3", A321003.getFornavn());
        assertNull(A321003.getAgressoStartDato());
        assertEquals(LocalDate.of(2022, 1, 10), A321003.getStartDato());
        assertEquals(LocalDate.of(2022, 1, 10), A321003.getRemedyStartDato());
    }
}