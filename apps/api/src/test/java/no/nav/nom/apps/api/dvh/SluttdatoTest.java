package no.nav.nom.apps.api.dvh;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

@DisplayName("Dvh.Sluttdato")
public class SluttdatoTest extends DvhTestBase {

    @Test
    @SneakyThrows
    @DisplayName("Tester regler for statlige ansatte")
    void scenario11() {
        // Fast statlig ansatt med sluttdato i agresso
        val test1Sluttdato = LocalDate.now().minusDays(10);
        val test1 = DvhRessurs.builder()
                .personIdent("test1")
                .navIdent("test1")
                .ansattRessursKode("F")
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(LocalDate.parse("2099-12-31"))
                .agressoSluttdato(test1Sluttdato)
                .remedyPermisjon(false)
                .build();
        Assertions.assertEquals(test1Sluttdato, test1.getSluttdato());

        val test2Sluttdato = LocalDate.now().minusDays(10);
        val test2 = DvhRessurs.builder()
                .personIdent("test2")
                .navIdent("test2")
                .ansattRessursKode("F")
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(test2Sluttdato)
                .agressoSluttdato(LocalDate.parse("2099-12-31"))
                .remedyPermisjon(false)
                .build();
        Assertions.assertEquals(test2Sluttdato, test2.getSluttdato());

        // Fast statlig ansatt med sluttdato i agresso og remedy
        val test3Sluttdato = LocalDate.now().minusDays(10);
        val test3 = DvhRessurs.builder()
                .personIdent("test3")
                .navIdent("test3")
                .ansattRessursKode("F")
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(test3Sluttdato.plusDays(20))
                .agressoSluttdato(test3Sluttdato)
                .remedyPermisjon(false)
                .build();

        Assertions.assertEquals(test3Sluttdato, test3.getSluttdato());
        // Fast statlig ansatt i remedy, men E i agresso med sluttdato i agresso og remedy
        val test4Sluttdato = LocalDate.now();
        val test4 = DvhRessurs.builder()
                .personIdent("test4")
                .navIdent("test4")
                .ansattRessursKode("E")
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(test4Sluttdato)
                .agressoSluttdato(test4Sluttdato.minusDays(20))
                .remedyPermisjon(false)
                .build();
        Assertions.assertEquals(test4Sluttdato, test4.getSluttdato());
    }

    @Test
    @SneakyThrows
    @DisplayName("Tester regler for kommunalt, eksternt og annen_statlig ansatte")
    void scenario21() {
        scenario21Helper("NAV Kommunalt", "1");
        scenario21Helper("Annen Statlig", "2");
        scenario21Helper("Eksternt", "3");
    }

    private void scenario21Helper(String remedyRessursType, String suffix) {
        val test1Sluttdato = LocalDate.now().minusDays(10);
        val test1 = DvhRessurs.builder()
                .personIdent("test1" + suffix)
                .navIdent("test1" + suffix)
                .ansattRessursKode(null)
                .remedyAnsattType(remedyRessursType)
                .remedySluttdato(test1Sluttdato)
                .agressoSluttdato(null)
                .remedyPermisjon(false)
                .build();
        Assertions.assertEquals(test1Sluttdato, test1.getSluttdato());


        val test2 = DvhRessurs.builder()
                .personIdent("test2" + suffix)
                .navIdent("test2" + suffix)
                .ansattRessursKode(null)
                .remedyAnsattType(remedyRessursType)
                .remedySluttdato(LocalDate.parse("2099-12-31"))
                .agressoSluttdato(null)
                .remedyPermisjon(false)
                .build();
        Assertions.assertNull(test2.getSluttdato());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sluttdato edgecase - ekstern, kommunal eller annenstat har tidligere vært statlig ansatt")
    void scenario22() {
        scenario22Helper("NAV Kommunalt", "10");
        scenario22Helper("Annen Statlig", "20");
        scenario22Helper("Eksternt", "30");
    }

    void scenario22Helper(String remedyRessursType, String suffix) {
        val test = DvhRessurs.builder()
                .personIdent("test" + suffix)
                .navIdent("test" + suffix)
                .ansattRessursKode("F")
                .remedyAnsattType(remedyRessursType)
                .remedySluttdato(LocalDate.parse("2099-12-31"))
                .agressoSluttdato(LocalDate.now().minusDays(20))
                .remedyPermisjon(false)
                .build();
        Assertions.assertNull(test.getSluttdato());
    }

    @Test
    @SneakyThrows
    @DisplayName("Tar hensyn til permisjon")
    /*
    Tar hensyn til ansatte som er i permisjon i remedy, og som er registert i agresso som noe annet enn ekstern
    samt at de enten er kommunal i remedy ELLER ikke har satt en sluttdato i agresso.

    OBSERVASJON: Det tas hensyn til permisjon for alle som er registret i agresso med unntak av de med kode "E"
     */
    void scenario31() {
        // Fast statlig ansatt i permisjon:
        val test1 = DvhRessurs.builder()
                .personIdent("test1")
                .navIdent("test1")
                .ansattRessursKode("F")
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(LocalDate.now())
                .agressoSluttdato(LocalDate.parse("2099-12-31"))
                .remedyPermisjon(true)
                .remedyAktiv(true)
                .build();
        Assertions.assertNull(test1.getSluttdato());
        // Fast kommunalt ansatt i permisjon:
        val test2 = DvhRessurs.builder()
                .personIdent("test2")
                .navIdent("test2")
                .ansattRessursKode(null)
                .remedyAnsattType("NAV Kommunalt")
                .remedySluttdato(LocalDate.now())
                .agressoSluttdato(null)
                .remedyPermisjon(true)
                .remedyAktiv(true)
                .build();
        Assertions.assertNull(test2.getSluttdato());
        // Fast statlig ansatt med sluttdato i agresso:
        val test3Sluttdato = LocalDate.now().minusDays(10);
        val test3 = DvhRessurs.builder()
                .personIdent("test3")
                .navIdent("test3")
                .ansattRessursKode("F")
                .remedyAnsattType("NAV Statlig")
                .remedySluttdato(LocalDate.now())
                .agressoSluttdato(test3Sluttdato)
                .remedyPermisjon(true)
                .remedyAktiv(true)
                .build();
        Assertions.assertEquals(test3Sluttdato, test3.getSluttdato());

        val test4Sluttdato = LocalDate.now().minusDays(10);
        val test4 = DvhRessurs.builder()
                .personIdent("test4")
                .navIdent("test4")
                .ansattRessursKode("E")
                .remedyAnsattType("Eksternt")
                .remedySluttdato(test4Sluttdato)
                .agressoSluttdato(null)
                .remedyPermisjon(true)
                .remedyAktiv(true)
                .build();
        Assertions.assertEquals(test4Sluttdato, test4.getSluttdato());
    }
}
