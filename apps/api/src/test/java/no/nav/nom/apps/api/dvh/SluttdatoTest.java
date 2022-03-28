package no.nav.nom.apps.api.dvh;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.DvhLegacyRessursUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

@DisplayName("Dvh.Sluttdato")
public class SluttdatoTest extends DvhTestBase {
    @Autowired
    private DvhLegacyRessursUtils dvhLegacyRessursUtils;

    @Test
    @SneakyThrows
    @DisplayName("Tester regler for statlige ansatte")
    void scenario11(){
        // Fast statlig ansatt med sluttdato i agresso
        val test1Sluttdato = LocalDate.now().minusDays(10);
        val test1 = DvhLegacyRessursUtils.LegacyResponseObject
                .builder()
                .personIdent("test1")
                .navIdent("test1")
                .agressoRessursType("F")
                .remedyRessursType("NAV Statlig")
                .remedySluttdato(LocalDate.parse("2099-12-31"))
                .agressoSluttdato(test1Sluttdato)
                .remedyPermisjon("NEI")
                .forventetSluttdato(test1Sluttdato)
                .build();
        // Fast statlig ansatt uten sluttdato i agresso, men sluttdato i remedy
        val test2Sluttdato = LocalDate.now().minusDays(10);
        val test2 = DvhLegacyRessursUtils.LegacyResponseObject
                .builder()
                .personIdent("test2")
                .navIdent("test2")
                .agressoRessursType("F")
                .remedyRessursType("NAV Statlig")
                .remedySluttdato(test2Sluttdato)
                .agressoSluttdato(LocalDate.parse("2099-12-31"))
                .remedyPermisjon("NEI")
                .forventetSluttdato(test2Sluttdato)
                .build();
        // Fast statlig ansatt med sluttdato i agresso og remedy
        val test3Sluttdato = LocalDate.now().minusDays(10);
        val test3 = DvhLegacyRessursUtils.LegacyResponseObject
                .builder()
                .personIdent("test3")
                .navIdent("test3")
                .agressoRessursType("F")
                .remedyRessursType("NAV Statlig")
                .remedySluttdato(test3Sluttdato.plusDays(20))
                .agressoSluttdato(test3Sluttdato)
                .remedyPermisjon("NEI")
                .forventetSluttdato(test3Sluttdato)
                .build();
        // Fast statlig ansatt i remedy, men E i agresso med sluttdato i agresso og remedy
        val test4Sluttdato = LocalDate.now();
        val test4 = DvhLegacyRessursUtils.LegacyResponseObject
                .builder()
                .personIdent("test4")
                .navIdent("test4")
                .agressoRessursType("E")
                .remedyRessursType("NAV Statlig")
                .remedySluttdato(test4Sluttdato)
                .agressoSluttdato(test4Sluttdato.minusDays(20))
                .remedyPermisjon("NEI")
                .forventetSluttdato(test4Sluttdato)
                .build();


        val response = List.of(test1, test2, test3, test4);
        dvhLegacyRessursUtils.addToResponse(response);
        dvhLegacyRessursUtils.mockResponse();
        response.forEach(this::assertSluttdato);
    }
    @Test
    @SneakyThrows
    @DisplayName("Tester regler for kommunalt, eksternt og annen_statlig ansatte")
    void scenario21(){
        val testList = new LinkedList<DvhLegacyRessursUtils.LegacyResponseObject>();
        testList.addAll(scenario21Helper("NAV Kommunalt", "1"));
        testList.addAll(scenario21Helper("Annen Statlig", "2"));
        testList.addAll(scenario21Helper("Eksternt", "3"));

        dvhLegacyRessursUtils.addToResponse(testList);
        dvhLegacyRessursUtils.mockResponse();
        testList.forEach(this::assertSluttdato);
        //scenario22();
    }

    private List<DvhLegacyRessursUtils.LegacyResponseObject> scenario21Helper(String remedyRessursType, String suffix){
        // Ikke statlig ansatt med sluttdato i remedy
        val test1Sluttdato = LocalDate.now().minusDays(10);
        val test1 = DvhLegacyRessursUtils.LegacyResponseObject
                .builder()
                .personIdent("test1"+suffix)
                .navIdent("test1"+suffix)
                .agressoRessursType(null)
                .remedyRessursType(remedyRessursType)
                .remedySluttdato(test1Sluttdato)
                .agressoSluttdato(null)
                .remedyPermisjon("NEI")
                .forventetSluttdato(test1Sluttdato)
                .build();
        // Ikke statlig ansatt uten sluttdato i remedy
        val test2 = DvhLegacyRessursUtils.LegacyResponseObject
                .builder()
                .personIdent("test2"+suffix)
                .navIdent("test2"+suffix)
                .agressoRessursType(null)
                .remedyRessursType(remedyRessursType)
                .remedySluttdato(LocalDate.parse("2099-12-31"))
                .agressoSluttdato(null)
                .remedyPermisjon("NEI")
                .forventetSluttdato(null)
                .build();


        return List.of(test1, test2);
    }

    @Test
    @SneakyThrows
    @DisplayName("Sluttdato edgecase - ekstern, kommunal eller annenstat har tidligere vært statlig ansatt")
    void scenario22(){
        scenario22Helper("NAV Kommunalt", "10");
        scenario22Helper("Annen Statlig", "20");
        scenario22Helper("Eksternt", "30");
    }

    void scenario22Helper(String remedyRessursType, String suffix){
        val test = DvhLegacyRessursUtils.LegacyResponseObject
                .builder()
                .personIdent("test"+suffix)
                .navIdent("test"+suffix)
                .agressoRessursType("F")
                .remedyRessursType(remedyRessursType)
                .remedySluttdato(LocalDate.parse("2099-12-31"))
                .agressoSluttdato(LocalDate.now().minusDays(20))
                .remedyPermisjon("NEI")
                .forventetSluttdato(null)
                .build();

        dvhLegacyRessursUtils.addToResponse(test);
        dvhLegacyRessursUtils.mockResponse();
        assertSluttdato(test);
    }

    @Test
    @SneakyThrows
    @DisplayName("Tar hensyn til permisjon")
    /*
    Tar hensyn til ansatte som er i permisjon i remedy, og som er registert i agresso som noe annet enn ekstern
    samt at de enten er kommunal i remedy ELLER ikke har satt en sluttdato i agresso.

    OBSERVASJON: Det tas hensyn til permisjon for alle som er registret i agresso med unntak av de med kode "E"
     */
    void scenario31(){
        // Fast statlig ansatt i permisjon:
        val test1 = DvhLegacyRessursUtils.LegacyResponseObject
                .builder()
                .personIdent("test1")
                .navIdent("test1")
                .agressoRessursType("F")
                .remedyRessursType("NAV Statlig")
                .remedySluttdato(LocalDate.now())
                .agressoSluttdato(LocalDate.parse("2099-12-31"))
                .remedyPermisjon("JA")
                .forventetSluttdato(null)
                .build();
        // Fast kommunalt ansatt i permisjon:
        val test2 = DvhLegacyRessursUtils.LegacyResponseObject
                .builder()
                .personIdent("test2")
                .navIdent("test2")
                .agressoRessursType(null)
                .remedyRessursType("NAV Kommunalt")
                .remedySluttdato(LocalDate.now())
                .agressoSluttdato(null)
                .remedyPermisjon("JA")
                .forventetSluttdato(null)
                .build();
        // Fast statlig ansatt med sluttdato i agresso:
        val test3Sluttdato = LocalDate.now().minusDays(10);
        val test3 = DvhLegacyRessursUtils.LegacyResponseObject
                .builder()
                .personIdent("test3")
                .navIdent("test3")
                .agressoRessursType("F")
                .remedyRessursType("NAV Statlig")
                .remedySluttdato(LocalDate.now())
                .agressoSluttdato(test3Sluttdato)
                .remedyPermisjon("JA")
                .forventetSluttdato(test3Sluttdato)
                .build();
        val test4Sluttdato = LocalDate.now().minusDays(10);
        val test4 = DvhLegacyRessursUtils.LegacyResponseObject
                .builder()
                .personIdent("test4")
                .navIdent("test4")
                .agressoRessursType("E")
                .remedyRessursType("Eksternt")
                .remedySluttdato(test4Sluttdato)
                .agressoSluttdato(null)
                .remedyPermisjon("JA")
                .forventetSluttdato(test4Sluttdato)
                .build();
        val response = List.of(test1, test2, test3, test4);
        dvhLegacyRessursUtils.addToResponse(response);
        dvhLegacyRessursUtils.mockResponse();
        response.forEach(this::assertSluttdato);
    }

    private void assertSluttdato(DvhLegacyRessursUtils.LegacyResponseObject legacyResponseObject){
        Assertions.assertEquals(legacyResponseObject.getForventetSluttdato(), dvhRepository.getSluttdatoByPersonIdent().get(legacyResponseObject.getPersonIdent()));
    }
}
