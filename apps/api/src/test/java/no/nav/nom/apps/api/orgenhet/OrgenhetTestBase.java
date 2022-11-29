package no.nav.nom.apps.api.orgenhet;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.DvhOrgenhetMock;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrgenhetTestBase extends TestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Autowired
    private IOrgenhetService orgenhetService;

    @Test
    @SneakyThrows
    @DisplayName("Innlastning av orgenheter fra DVH med forskjellig agressoid")
    void scenario11() {
        val mockedEnhet1 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId1")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .build();
        val mockedEnhet2 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId2")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .build();
        val mockedEnhet3 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId3")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2, mockedEnhet3);

        dvhRestConsumer.loadFromDvh();

        assertEquals(3, orgenhetService.getAllEnheter().size());

        val outputEnhet = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet1.getORGENHET_KODE(),
                mockedEnhet1.getORGNODE_NIV(),
                mockedEnhet1.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet.isPresent());

        assertEquals(mockedEnhet1.getORGENHET_KODE(), outputEnhet.get().getAgressoId());
        assertEquals(mockedEnhet1.getENHETTYPE_NAVN(), outputEnhet.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet1.getORGNODE_NIV(), outputEnhet.get().getOrgNiv());

        val outputEnhet2 = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet2.getORGENHET_KODE(),
                mockedEnhet2.getORGNODE_NIV(),
                mockedEnhet2.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet2.isPresent());

        assertEquals(mockedEnhet2.getORGENHET_KODE(), outputEnhet2.get().getAgressoId());
        assertEquals(mockedEnhet2.getENHETTYPE_NAVN(), outputEnhet2.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet2.getORGNODE_NIV(), outputEnhet2.get().getOrgNiv());

        val outputEnhet3 = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet3.getORGENHET_KODE(),
                mockedEnhet3.getORGNODE_NIV(),
                mockedEnhet3.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet3.isPresent());

        assertEquals(mockedEnhet3.getORGENHET_KODE(), outputEnhet3.get().getAgressoId());
        assertEquals(mockedEnhet3.getENHETTYPE_NAVN(), outputEnhet3.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet3.getORGNODE_NIV(), outputEnhet3.get().getOrgNiv());
    }

    @Test
    @SneakyThrows
    @DisplayName("Innlastning av orgenheter fra DVH med forskjellig navn")
    void scenario12() {
        val commonEnhetBuilder = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId").ORGNODE_NIV("ORGENHET").ENHETTYPE_NAVN("TYPEEE");
        val mockedEnhet1 = commonEnhetBuilder.ORGENHET_NAVN("testNavn1").build();
        val mockedEnhet2 = commonEnhetBuilder.ORGENHET_NAVN("testNavn2").build();
        val mockedEnhet3 = commonEnhetBuilder.ORGENHET_NAVN("testNavn3").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2, mockedEnhet3);

        dvhRestConsumer.loadFromDvh();

        assertEquals(3, orgenhetService.getAllEnheter().size());

        val outputEnhet = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet1.getORGENHET_KODE(),
                mockedEnhet1.getORGNODE_NIV(),
                mockedEnhet1.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet.isPresent());

        assertEquals(mockedEnhet1.getORGENHET_KODE(), outputEnhet.get().getAgressoId());
        assertEquals(mockedEnhet1.getENHETTYPE_NAVN(), outputEnhet.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet1.getORGNODE_NIV(), outputEnhet.get().getOrgNiv());

        val outputEnhet2 = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet2.getORGENHET_KODE(),
                mockedEnhet2.getORGNODE_NIV(),
                mockedEnhet2.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet2.isPresent());

        assertEquals(mockedEnhet2.getORGENHET_KODE(), outputEnhet2.get().getAgressoId());
        assertEquals(mockedEnhet2.getENHETTYPE_NAVN(), outputEnhet2.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet2.getORGNODE_NIV(), outputEnhet2.get().getOrgNiv());

        val outputEnhet3 = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet3.getORGENHET_KODE(),
                mockedEnhet3.getORGNODE_NIV(),
                mockedEnhet3.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet3.isPresent());

        assertEquals(mockedEnhet3.getORGENHET_KODE(), outputEnhet3.get().getAgressoId());
        assertEquals(mockedEnhet3.getENHETTYPE_NAVN(), outputEnhet3.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet3.getORGNODE_NIV(), outputEnhet3.get().getOrgNiv());
    }

    @Test
    @SneakyThrows
    @DisplayName("Innlastning av orgenheter fra DVH med forskjellig nivå")
    void scenario13() {
        val commonEnhetBuilder = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId").ORGENHET_NAVN("testNavn").ENHETTYPE_NAVN("TYPEEE");
        val mockedEnhet1 = commonEnhetBuilder.ORGNODE_NIV("ORGNIV1").build();
        val mockedEnhet2 = commonEnhetBuilder.ORGNODE_NIV("ORGNIV3").build();
        val mockedEnhet3 = commonEnhetBuilder.ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2, mockedEnhet3);

        dvhRestConsumer.loadFromDvh();

        assertEquals(3, orgenhetService.getAllEnheter().size());

        val outputEnhet = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet1.getORGENHET_KODE(),
                mockedEnhet1.getORGNODE_NIV(),
                mockedEnhet1.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet.isPresent());

        assertEquals(mockedEnhet1.getORGENHET_KODE(), outputEnhet.get().getAgressoId());
        assertEquals(mockedEnhet1.getENHETTYPE_NAVN(), outputEnhet.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet1.getORGNODE_NIV(), outputEnhet.get().getOrgNiv());

        val outputEnhet2 = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet2.getORGENHET_KODE(),
                mockedEnhet2.getORGNODE_NIV(),
                mockedEnhet2.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet2.isPresent());

        assertEquals(mockedEnhet2.getORGENHET_KODE(), outputEnhet2.get().getAgressoId());
        assertEquals(mockedEnhet2.getENHETTYPE_NAVN(), outputEnhet2.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet2.getORGNODE_NIV(), outputEnhet2.get().getOrgNiv());

        val outputEnhet3 = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet3.getORGENHET_KODE(),
                mockedEnhet3.getORGNODE_NIV(),
                mockedEnhet3.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet3.isPresent());

        assertEquals(mockedEnhet3.getORGENHET_KODE(), outputEnhet3.get().getAgressoId());
        assertEquals(mockedEnhet3.getENHETTYPE_NAVN(), outputEnhet3.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet3.getORGNODE_NIV(), outputEnhet3.get().getOrgNiv());
    }

    @Test
    @SneakyThrows
    @DisplayName("Dupliserende innenfor 1 mnd slås sammen")
    void scenario21() {
        val mockedEnhet1 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202108)
                .ORGENHET_TIL_PERIODE(202112)
                .build();
        val mockedEnhet2 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202201)
                .ORGENHET_TIL_PERIODE(202204)
                .build();
        val mockedEnhet3 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202205)
                .build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2, mockedEnhet3);

        dvhRestConsumer.loadFromDvh();

        assertEquals(1, orgenhetService.getAllEnheter().size());

        val outputEnhet = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet1.getORGENHET_KODE(),
                mockedEnhet1.getORGNODE_NIV(),
                mockedEnhet1.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet.isPresent());

        assertEquals(mockedEnhet1.getORGENHET_KODE(), outputEnhet.get().getAgressoId());
        assertEquals(mockedEnhet1.getENHETTYPE_NAVN(), outputEnhet.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet1.getORGNODE_NIV(), outputEnhet.get().getOrgNiv());
        assertEquals(LocalDate.parse("2021-08-01"), outputEnhet.get().getGyldigFom());
        assertFalse(outputEnhet.get().getGyldigTom().isPresent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Dupliserende utenfor 1 mnd slås ikke sammen")
    void scenario22() {
        val mockedEnhet1 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202108)
                .ORGENHET_TIL_PERIODE(202112)
                .build();
        val mockedEnhet2 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202202)
                .ORGENHET_TIL_PERIODE(202204)
                .build();
        val mockedEnhet3 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202206)
                .build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2, mockedEnhet3);

        dvhRestConsumer.loadFromDvh();

        assertEquals(3, orgenhetService.getAllEnheter().size());

        val outputEnhet = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet3.getORGENHET_KODE(),
                mockedEnhet3.getORGNODE_NIV(),
                mockedEnhet3.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet.isPresent());

        assertEquals(mockedEnhet1.getORGENHET_KODE(), outputEnhet.get().getAgressoId());
        assertEquals(mockedEnhet1.getENHETTYPE_NAVN(), outputEnhet.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet1.getORGNODE_NIV(), outputEnhet.get().getOrgNiv());
        assertEquals(LocalDate.parse("2022-06-01"), outputEnhet.get().getGyldigFom());
        assertFalse(outputEnhet.get().getGyldigTom().isPresent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Endringer på persistert enhet")
    void scenario31() {
        val mockedEnhet1 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202108)
                .build();

        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1);

        dvhRestConsumer.loadFromDvh();

        assertEquals(1, orgenhetService.getAllEnheter().size());

        val outputEnhet = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet1.getORGENHET_KODE(),
                mockedEnhet1.getORGNODE_NIV(),
                mockedEnhet1.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet.isPresent());

        assertEquals(mockedEnhet1.getORGENHET_KODE(), outputEnhet.get().getAgressoId());
        assertEquals(mockedEnhet1.getENHETTYPE_NAVN(), outputEnhet.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet1.getORGNODE_NIV(), outputEnhet.get().getOrgNiv());
        assertEquals(LocalDate.parse("2021-08-01"), outputEnhet.get().getGyldigFom());
        assertFalse(outputEnhet.get().getGyldigTom().isPresent());

        dvhOrgenhetMockUtils.createEmptyResponse();

        val mockedEnhet2 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202108)
                .ORGENHET_TIL_PERIODE(202201)
                .build();

        dvhOrgenhetMockUtils.addToResponse(mockedEnhet2);

        dvhRestConsumer.loadFromDvh();

        assertEquals(1, orgenhetService.getAllEnheter().size());

        val outputEnhet2 = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet1.getORGENHET_KODE(),
                mockedEnhet1.getORGNODE_NIV(),
                mockedEnhet1.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet2.isPresent());

        assertEquals(mockedEnhet1.getORGENHET_KODE(), outputEnhet2.get().getAgressoId());
        assertEquals(mockedEnhet1.getENHETTYPE_NAVN(), outputEnhet2.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet1.getORGNODE_NIV(), outputEnhet2.get().getOrgNiv());
        assertEquals(LocalDate.parse("2021-08-01"), outputEnhet2.get().getGyldigFom());
        assertTrue(outputEnhet2.get().getGyldigTom().isPresent());
        assertEquals(LocalDate.parse("2022-01-31"), outputEnhet2.get().getGyldigTom().get());
    }

    @Test
    @SneakyThrows
    @DisplayName("Endringer på historisk persistert enhet")
    void scenario32() {
        val mockedEnhet1 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202008)
                .ORGENHET_TIL_PERIODE(202012)
                .build();

        val mockedEnhet2 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202106)
                .build();

        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);

        dvhRestConsumer.loadFromDvh();

        val allEnheter = orgenhetService.getAllEnheter();
        assertEquals(2, allEnheter.size());

        val outputEnhet = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet1.getORGENHET_KODE(),
                mockedEnhet1.getORGNODE_NIV(),
                mockedEnhet1.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet.isPresent());

        assertEquals(mockedEnhet1.getORGENHET_KODE(), outputEnhet.get().getAgressoId());
        assertEquals(mockedEnhet1.getENHETTYPE_NAVN(), outputEnhet.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet1.getORGNODE_NIV(), outputEnhet.get().getOrgNiv());
        assertEquals(LocalDate.parse("2021-06-01"), outputEnhet.get().getGyldigFom());
        assertFalse(outputEnhet.get().getGyldigTom().isPresent());

        val historicFid = allEnheter.stream().filter(e -> !e.getFid().equals(outputEnhet.get().getFid())).findFirst().get().getFid();

        var historicOutputEnhet = orgenhetService.getByFid(historicFid);
        assertTrue(historicOutputEnhet.isPresent());
        assertEquals(LocalDate.parse("2020-12-31"), historicOutputEnhet.get().getGyldigTom().get());

        dvhOrgenhetMockUtils.createEmptyResponse();
        mockedEnhet1.setORGENHET_TIL_PERIODE(202102);
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);

        dvhRestConsumer.loadFromDvh();

        historicOutputEnhet = orgenhetService.getByFid(historicFid);
        assertTrue(historicOutputEnhet.isPresent());
        assertEquals(LocalDate.parse("2021-02-28"), historicOutputEnhet.get().getGyldigTom().get());
    }

    @Test
    @SneakyThrows
    @DisplayName("Endringer på persistert enhet skaper split")
    void scenario33() {
        val mockedEnhet1 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202008)
                .ORGENHET_TIL_PERIODE(202012)
                .build();

        val mockedEnhet2 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202101)
                .build();

        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);

        dvhRestConsumer.loadFromDvh();

        var allEnheter = orgenhetService.getAllEnheter();
        assertEquals(1, allEnheter.size());

        val outputEnhet1 = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet1.getORGENHET_KODE(),
                mockedEnhet1.getORGNODE_NIV(),
                mockedEnhet1.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet1.isPresent());

        assertEquals(mockedEnhet1.getORGENHET_KODE(), outputEnhet1.get().getAgressoId());
        assertEquals(mockedEnhet1.getENHETTYPE_NAVN(), outputEnhet1.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet1.getORGNODE_NIV(), outputEnhet1.get().getOrgNiv());
        assertEquals(LocalDate.parse("2020-08-01"), outputEnhet1.get().getGyldigFom());
        assertFalse(outputEnhet1.get().getGyldigTom().isPresent());

        mockedEnhet1.setORGENHET_TIL_PERIODE(202011);
        dvhOrgenhetMockUtils.createEmptyResponse();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);
        dvhRestConsumer.loadFromDvh();

        allEnheter = orgenhetService.getAllEnheter();
        assertEquals(2, allEnheter.size());

        val outputEnhet2 = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet1.getORGENHET_KODE(),
                mockedEnhet1.getORGNODE_NIV(),
                mockedEnhet1.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet2.isPresent());

        assertEquals(outputEnhet1.get().getFid(), outputEnhet2.get().getFid());
        assertEquals(LocalDate.parse("2021-01-01"), outputEnhet2.get().getGyldigFom());
        assertFalse(outputEnhet2.get().getGyldigTom().isPresent());

        val outputEnhet3 = allEnheter.stream().filter(e -> !e.getFid().equals(outputEnhet2.get().getFid())).findAny();
        assertTrue(outputEnhet3.isPresent());
        assertEquals(LocalDate.parse("2020-08-01"), outputEnhet3.get().getGyldigFom());
        assertEquals(LocalDate.parse("2020-11-30"), outputEnhet3.get().getGyldigTom().get());
    }

    @Test
    @SneakyThrows
    @DisplayName("Endringer på persistert enhet skaper merge - gjøres ikke noe med, må manuelt løses")
    void scenario34() {
        val mockedEnhet1 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202008)
                .ORGENHET_TIL_PERIODE(202011)
                .build();

        val mockedEnhet2 = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202101)
                .build();

        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);

        dvhRestConsumer.loadFromDvh();

        var allEnheter = orgenhetService.getAllEnheter();
        assertEquals(2, allEnheter.size());

        mockedEnhet1.setORGENHET_TIL_PERIODE(202012);
        dvhOrgenhetMockUtils.createEmptyResponse();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);
        dvhRestConsumer.loadFromDvh();

        allEnheter = orgenhetService.getAllEnheter();
        assertEquals(2, allEnheter.size());

        val outputEnhet1 = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet1.getORGENHET_KODE(),
                mockedEnhet1.getORGNODE_NIV(),
                mockedEnhet1.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet1.isPresent());

        assertEquals(mockedEnhet1.getORGENHET_KODE(), outputEnhet1.get().getAgressoId());
        assertEquals(mockedEnhet1.getENHETTYPE_NAVN(), outputEnhet1.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet1.getORGNODE_NIV(), outputEnhet1.get().getOrgNiv());
        assertEquals(LocalDate.parse("2021-01-01"), outputEnhet1.get().getGyldigFom());
        assertFalse(outputEnhet1.get().getGyldigTom().isPresent());

        val outputEnhet3 = allEnheter.stream().filter(e -> !e.getFid().equals(outputEnhet1.get().getFid())).findAny();
        assertTrue(outputEnhet3.isPresent());
        assertEquals(LocalDate.parse("2020-08-01"), outputEnhet3.get().getGyldigFom());
        assertEquals(LocalDate.parse("2020-11-30"), outputEnhet3.get().getGyldigTom().get());
    }

}
