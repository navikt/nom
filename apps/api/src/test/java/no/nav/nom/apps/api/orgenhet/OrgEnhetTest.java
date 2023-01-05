package no.nav.nom.apps.api.orgenhet;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.DvhOrgEnhetMock;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.Optional;

import static no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhDataLoader.UNLEASH_TOGGLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrgEnhetTest extends TestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Autowired
    private IOrgenhetService orgenhetService;

    @Autowired
    private OrgEnhetStateService orgEnhetStateService;

    @MockBean
    private OrgEnhetCommandService orgEnhetCommandService;

    @BeforeEach
    void resetOrgEnhetState() {
        orgEnhetStateService.clear();
    }

    @Test
    @SneakyThrows
    @DisplayName("Innlastning av orgenheter fra DVH med forskjellig agressoid")
    void scenario11() {
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId1")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .build();
        val mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId2")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .build();
        val mockedEnhet3 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId3")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2, mockedEnhet3);

        dvhRestConsumer.loadRessurserOgOrgEnheter();
        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            verify(orgEnhetCommandService, times(3)).opprettOrgEnhet(any());
        }

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
        val commonEnhetBuilder = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId").ORGNODE_NIV("ORGENHET").ENHETTYPE_NAVN("TYPEEE");
        val mockedEnhet1 = commonEnhetBuilder.ORGENHET_NAVN("testNavn1").build();
        val mockedEnhet2 = commonEnhetBuilder.ORGENHET_NAVN("testNavn2").build();
        val mockedEnhet3 = commonEnhetBuilder.ORGENHET_NAVN("testNavn3").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2, mockedEnhet3);

        dvhRestConsumer.loadRessurserOgOrgEnheter();
        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            verify(orgEnhetCommandService, times(3)).opprettOrgEnhet(any());
        }


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
        val commonEnhetBuilder = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId").ORGENHET_NAVN("testNavn").ENHETTYPE_NAVN("TYPEEE");
        val mockedEnhet1 = commonEnhetBuilder.ORGNODE_NIV("ORGNIV1").build();
        val mockedEnhet2 = commonEnhetBuilder.ORGNODE_NIV("ORGNIV3").build();
        val mockedEnhet3 = commonEnhetBuilder.ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2, mockedEnhet3);

        dvhRestConsumer.loadRessurserOgOrgEnheter();
        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            verify(orgEnhetCommandService, times(3)).opprettOrgEnhet(any());
        }

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
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202108)
                .ORGENHET_TIL_PERIODE(202112)
                .build();
        val mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202201)
                .ORGENHET_TIL_PERIODE(202204)
                .build();
        val mockedEnhet3 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202205)
                .build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2, mockedEnhet3);

        dvhRestConsumer.loadRessurserOgOrgEnheter();
        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            verify(orgEnhetCommandService, times(1)).opprettOrgEnhet(any());
        }


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
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202108)
                .ORGENHET_TIL_PERIODE(202112)
                .build();
        val mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202202)
                .ORGENHET_TIL_PERIODE(202204)
                .build();
        val mockedEnhet3 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202206)
                .build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2, mockedEnhet3);

        dvhRestConsumer.loadRessurserOgOrgEnheter();

        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            verify(orgEnhetCommandService, times(3)).opprettOrgEnhet(any());
            verify(orgEnhetCommandService, times(2)).avikleOrgEnhet(any(), any());
        }


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
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202108)
                .build();

        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            orgEnhetStateService.putEventAggregate(
                new OrgEnhetState(
                    "orgenhet_FID_1",
                    "testId",
                    "testNavn",
                    LocalDate.of(2021, 8, 1),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of("testId-testNavn-ORGENHET"),
                    Optional.of("testId"),
                    Optional.of("ORGNIV"),
                    Optional.of("ORGENHET")
            ));
        }
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

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
        val mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202108)
                .ORGENHET_TIL_PERIODE(202201)
                .build();

        dvhOrgenhetMockUtils.addToResponse(mockedEnhet2);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            verify(orgEnhetCommandService, times(0)).opprettOrgEnhet(any());
            verify(orgEnhetCommandService, times(1)).avikleOrgEnhet(any(), any());
        }

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
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202008)
                .ORGENHET_TIL_PERIODE(202012)
                .build();

        val mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202106)
                .build();

        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            verify(orgEnhetCommandService, times(2)).opprettOrgEnhet(any());
            verify(orgEnhetCommandService, times(1)).avikleOrgEnhet(any(), any());

            orgEnhetStateService.putEventAggregate(
                    new OrgEnhetState(
                            "orgenhet_FID_1",
                            "testId",
                            "testNavn",
                            LocalDate.of(2021, 6, 1),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.of("testId-testNavn-ORGENHET"),
                            Optional.of("testId"),
                            Optional.of("ORGNIV"),
                            Optional.of("ORGENHET")
                    ));
            orgEnhetStateService.putEventAggregate(
                    new OrgEnhetState(
                            "orgenhet_FID_2",
                            "testId",
                            "testNavn",
                            LocalDate.of(2020, 8, 1),
                            Optional.of(LocalDate.of(2020, 12, 31)),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.of("testId-testNavn-ORGENHET"),
                            Optional.of("testId"),
                            Optional.of("ORGNIV"),
                            Optional.of("ORGENHET")
                    ));


            val allEnheter = orgEnhetStateService.getAll();
            assertEquals(2, allEnheter.size());
            val outputEnhet = orgEnhetStateService.findNewestUniqueAgressoId("testId-testNavn-ORGENHET");
            assertTrue(outputEnhet.isPresent());
            assertEquals(LocalDate.parse("2021-06-01"), outputEnhet.get().gyldigFom());
            assertFalse(outputEnhet.get().gyldigTom().isPresent());

            var historicOutputEnhet = orgEnhetStateService.findByFid("orgenhet_FID_2");
            assertTrue(historicOutputEnhet.isPresent());
            assertEquals(LocalDate.parse("2020-12-31"), historicOutputEnhet.get().gyldigTom().get());
        }
        var allEnheter = orgenhetService.getAllEnheter();
        assertEquals(2, allEnheter.size());

        var outputEnhet1 = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet1.getORGENHET_KODE(),
                mockedEnhet1.getORGNODE_NIV(),
                mockedEnhet1.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet1.isPresent());

        assertEquals(mockedEnhet1.getORGENHET_KODE(), outputEnhet1.get().getAgressoId());
        assertEquals(mockedEnhet1.getENHETTYPE_NAVN(), outputEnhet1.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet1.getORGNODE_NIV(), outputEnhet1.get().getOrgNiv());
        assertEquals(LocalDate.parse("2021-06-01"), outputEnhet1.get().getGyldigFom());
        assertFalse(outputEnhet1.get().getGyldigTom().isPresent());

        val historicFid = allEnheter.stream().filter(e -> !e.getFid().equals(outputEnhet1.get().getFid())).findFirst().get().getFid();

        var historicOutputEnhet = orgenhetService.getByFid(historicFid);
        assertTrue(historicOutputEnhet.isPresent());
        assertEquals(LocalDate.parse("2020-12-31"), historicOutputEnhet.get().getGyldigTom().get());

        dvhOrgenhetMockUtils.createEmptyResponse();
        mockedEnhet1.setORGENHET_TIL_PERIODE(202102);
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);

        dvhRestConsumer.loadRessurserOgOrgEnheter();

        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            verify(orgEnhetCommandService, times(2)).opprettOrgEnhet(any());
            verify(orgEnhetCommandService, times(1)).avikleOrgEnhet(any(), any());
            verify(orgEnhetCommandService, times(1)).korrigerOrgEnhet(any(), any());

            orgEnhetStateService.putEventAggregate(
                    new OrgEnhetState(
                            "orgenhet_FID_1",
                            "testId",
                            "testNavn",
                            LocalDate.of(2020, 8, 1),
                            Optional.of(LocalDate.of(2021, 2, 28)),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.of("testId-testNavn-ORGENHET"),
                            Optional.of("testId"),
                            Optional.of("ORGNIV"),
                            Optional.of("ORGENHET")
                    ));


            val allStateEnheter = orgEnhetStateService.getAll();
            assertEquals(2, allStateEnheter.size());
            assertEquals(2, orgEnhetStateService.findAllByUniqueAgressoId("testId-testNavn-ORGENHET").size());

            var historicOutputStateEnhet = orgEnhetStateService.findByFid("orgenhet_FID_1");
            assertTrue(historicOutputStateEnhet.isPresent());
            assertEquals(LocalDate.parse("2021-02-28"), historicOutputStateEnhet.get().gyldigTom().get());
        }
        allEnheter = orgenhetService.getAllEnheter();
        assertEquals(2, allEnheter.size());

        var outputEnhet2 = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet1.getORGENHET_KODE(),
                mockedEnhet1.getORGNODE_NIV(),
                mockedEnhet1.getORGENHET_NAVN()
        );

        var historicStateFid = allEnheter.stream().filter(e -> !e.getFid().equals(outputEnhet2.get().getFid())).findFirst().get().getFid();

        historicOutputEnhet = orgenhetService.getByFid(historicStateFid);
        assertTrue(historicOutputEnhet.isPresent());
        assertEquals(LocalDate.parse("2021-02-28"), historicOutputEnhet.get().getGyldigTom().get());
    }

    @Test
    @SneakyThrows
    @DisplayName("Endringer på persistert enhet skaper split")
    void scenario33() {
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202008)
                .ORGENHET_TIL_PERIODE(202012)
                .build();

        val mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202101)
                .build();

        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        val outputEnhet1 = orgenhetService.getEnhetByAgressoIdNivaAndName(
                mockedEnhet1.getORGENHET_KODE(),
                mockedEnhet1.getORGNODE_NIV(),
                mockedEnhet1.getORGENHET_NAVN()
        );

        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            // DvhEnheter er slått samme der det er overlapp
            // Prossesering av dvhEnheter er seneste start dato først (start dato decend)

            // Mock1+2 er slått sammen i parsing.
            // Lager 'FID_1' [2020.08 - ]
            verify(orgEnhetCommandService, times(1)).opprettOrgEnhet(any());
            verify(orgEnhetCommandService, times(0)).avikleOrgEnhet(any(), any());

            orgEnhetStateService.putEventAggregate(
                    new OrgEnhetState(
                            "FID_1",
                            "testId",
                            "testNavn",
                            LocalDate.of(2020, 8, 1),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.of("testId-testNavn-ORGENHET"),
                            Optional.of("testId"),
                            Optional.of("ORGNIV"),
                            Optional.of("ORGENHET")
                    ));

            var allEnheter = orgEnhetStateService.getAll();
            assertEquals(1, allEnheter.size());

            var stateEnehet1 = orgEnhetStateService.findNewestUniqueAgressoId("testId-testNavn-ORGENHET");
            assertTrue(outputEnhet1.isPresent());
            assertEquals(LocalDate.parse("2020-08-01"), stateEnehet1.get().gyldigFom());
            assertFalse(stateEnehet1.get().gyldigTom().isPresent());
        }
        // Uten Toggle
        var allEnheter = orgenhetService.getAllEnheter();
        assertEquals(1, allEnheter.size());

        assertTrue(outputEnhet1.isPresent());

        assertEquals(mockedEnhet1.getORGENHET_KODE(), outputEnhet1.get().getAgressoId());
        assertEquals(mockedEnhet1.getENHETTYPE_NAVN(), outputEnhet1.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet1.getORGNODE_NIV(), outputEnhet1.get().getOrgNiv());
        assertEquals(LocalDate.parse("2020-08-01"), outputEnhet1.get().getGyldigFom());
        assertFalse(outputEnhet1.get().getGyldigTom().isPresent());

        mockedEnhet1.setORGENHET_TIL_PERIODE(202011);
        dvhOrgenhetMockUtils.createEmptyResponse();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);
        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            when(orgEnhetCommandService.opprettOrgEnhet(argThat(arg -> arg.gyldigFom().equals(LocalDate.of(2020,8,1))))).thenReturn("FID_2");
        }

        dvhRestConsumer.loadRessurserOgOrgEnheter();

        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            var stateEnehet1 = orgEnhetStateService.findNewestUniqueAgressoId("testId-testNavn-ORGENHET");
            final String firstEnhetFID = stateEnehet1.get().fid();


            // Før ny innlesning - En enhet i state : [2020.08 - ]
            // Prossesering av dvhEnheter er seneste start dato først (start dato decend). Altså mock 2 først
            // Parsing slår ikke sammen
            // leser mockEnhet2 [2021.01 - ] -> Finner overlapp på 'FID_1' og korrigerer startdato
                // State på FID_1 er fortsatt [2020.08 - ] -> Eventually [2021.01 - ]
            // mockEnhet1 er [2020.08 - 2020.11] -> Finner overlapp på 'FID_1' -> terminerer denne !!
                    // mockEnhet1 er [2020.08 - 2020.11] -> Finner overlapp på eventually fra dato - lager 'FID_2' og terminerer denne
            // Etter innlesning -
            //      FID_1: [2021.01 - ]
            //      FID_2: [2020.08 - 2020.11]

            verify(orgEnhetCommandService, times(1 + 1)).opprettOrgEnhet(argThat(arg -> arg.gyldigFom().equals(LocalDate.of(2020,8,1))));
            verify(orgEnhetCommandService, times(1)).avikleOrgEnhet(argThat(arg -> !arg.equals(firstEnhetFID)), argThat(arg -> arg.gyldigTom().equals(LocalDate.of(2020,11,30))));
            verify(orgEnhetCommandService, times(1)).korrigerOrgEnhet(any(), any());

            orgEnhetStateService.putEventAggregate(
                    new OrgEnhetState(
                            "FID_1",
                            "testId",
                            "testNavn",
                            LocalDate.of(2021, 1, 1),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.of("testId-testNavn-ORGENHET"),
                            Optional.of("testId"),
                            Optional.of("ORGNIV"),
                            Optional.of("ORGENHET")
                    ));
            orgEnhetStateService.putEventAggregate(
                    new OrgEnhetState(
                            "FID_2",
                            "testId",
                            "testNavn",
                            LocalDate.of(2020, 8, 1),
                            Optional.of(LocalDate.of(2020, 11, 30)),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.of("testId-testNavn-ORGENHET"),
                            Optional.of("testId"),
                            Optional.of("ORGNIV"),
                            Optional.of("ORGENHET")
                    ));

            var allStateEnheter = orgEnhetStateService.getAll();
            assertEquals(2, allStateEnheter.size());
        }
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
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202008)
                .ORGENHET_TIL_PERIODE(202011)
                .build();

        val mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE")
                .ORGENHET_FRA_PERIODE(202101)
                .build();

        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            verify(orgEnhetCommandService, times(2)).opprettOrgEnhet(any());
            verify(orgEnhetCommandService, times(1)).avikleOrgEnhet(any(), any());

            orgEnhetStateService.putEventAggregate(
                    new OrgEnhetState(
                            "orgenhet_FID_1",
                            "testId",
                            "testNavn",
                            LocalDate.of(2021, 1, 1),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.of("testId-testNavn-ORGENHET"),
                            Optional.of("testId"),
                            Optional.of("ORGNIV"),
                            Optional.of("ORGENHET")
                    ));
            orgEnhetStateService.putEventAggregate(
                    new OrgEnhetState(
                            "orgenhet_FID_2",
                            "testId",
                            "testNavn",
                            LocalDate.of(2020, 8, 1),
                            Optional.of(LocalDate.of(2020, 11, 30)),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.of("testId-testNavn-ORGENHET"),
                            Optional.of("testId"),
                            Optional.of("ORGNIV"),
                            Optional.of("ORGENHET")
                    ));
        }


        var allEnheter = orgenhetService.getAllEnheter();
        assertEquals(2, allEnheter.size());

        mockedEnhet1.setORGENHET_TIL_PERIODE(202012);
        dvhOrgenhetMockUtils.createEmptyResponse();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            verify(orgEnhetCommandService, times(2)).opprettOrgEnhet(any());
            verify(orgEnhetCommandService, times(1)).avikleOrgEnhet(any(), any());
        }

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
