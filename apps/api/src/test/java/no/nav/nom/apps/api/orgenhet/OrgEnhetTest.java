package no.nav.nom.apps.api.orgenhet;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.DvhOrgEnhetMock;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.orgenhet.eventsourcing.consumer.OrgEnhetEventSourceListener;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.AvvikleOrgEnhet;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.NomNivaaEndretEventV1;
import no.nav.nom.contract.nom.eventsource.NomNivaaEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// TODO: Fiks disse testene - ingen grunn til så mye test gjennom mange ledd/sevicer
public class OrgEnhetTest extends OrgEnhetTestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Autowired
    private OrgEnhetStateRepository orgEnhetStateRepository;

    @Autowired
    private OrgenhetService orgenhetService;

    @MockitoBean
    private OrgEnhetCommandService orgEnhetCommandService;

    @Captor
    ArgumentCaptor<AvvikleOrgEnhet> avvikleOrgEnhetCaptor;

    @BeforeEach
    void resetOrgEnhetState() {
        when(orgEnhetCommandService.opprettOrgEnhet(any())).thenReturn("DUMMY_FID");
        orgEnhetStateRepository.clear();
    }

    @Test
    void orgEnhetEventsArePassedToHandlers() {
        var orgEnhetTypeHandler = mock(OrgEnhetTypeHandler.class);
        var orgEnhetNomNivaaHandler = mock(OrgEnhetNomNivaaHandler.class);
        var orgEnhetEventSourceListener = new OrgEnhetEventSourceListener(
            orgEnhetTypeHandler,
            orgEnhetNomNivaaHandler,
            mock(no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor.class)
        );
        var typeMetadata = new InputMetadata("test", "type", Map.of(), null);
        var nivaaMetadata = new InputMetadata("test", "nivaa", Map.of(), null);
        var typeEvent = new OrgEnhetTypeEndretEventV1(
            "TYPE_FID",
            new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData("TYPE_ID", OrgEnhetsType.FYLKE),
            typeMetadata
        );
        var nivaaEvent = new NomNivaaEndretEventV1(
            "NIVAA_FID",
            new NomNivaaEndretEventV1.NomNivaaEndretEventData("NIVAA_ID", NomNivaa.DRIFTSENHET),
            nivaaMetadata
        );
        var records = records(typeEvent, nivaaEvent);

        orgEnhetEventSourceListener.onMessage(records, null);

        verify(orgEnhetTypeHandler).orgEnhetsTypeChanged("TYPE_FID", OrgEnhetsType.FYLKE);
        verify(orgEnhetNomNivaaHandler).mapOrgEnhetTypeToNewNomNivaaAndUpdateChildren("TYPE_FID", OrgEnhetsType.FYLKE);
        verify(orgEnhetNomNivaaHandler).ensureArbeidsomraadePresentUnderDriftsenhet("NIVAA_FID", NomNivaa.DRIFTSENHET);
    }

    private ConsumerRecords<String, OrgEnhetEvent> records(OrgEnhetEvent... events) {
        var topicPartition = new TopicPartition("events", 0);
        var eventRecords = java.util.stream.IntStream.range(0, events.length)
            .mapToObj(index -> new ConsumerRecord<>("events", 0, index, "key", events[index]))
            .toList();
        return new ConsumerRecords<>(Map.of(topicPartition, eventRecords));
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

        verify(orgEnhetCommandService, times(3)).opprettOrgEnhet(any());

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        // This testing should be split up
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202204", null));
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet2, 2, "202204", null));
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet3, 3, "202204", null));

        assertEquals(3, orgenhetService.getAllEnheter().size());

        var outputEnhet = orgenhetService.getEnhetByAgressoIdNivaAndName(
            mockedEnhet1.getORGENHET_KODE(),
            mockedEnhet1.getORGNODE_NIV(),
            mockedEnhet1.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet.isPresent());

        assertEquals(mockedEnhet1.getORGENHET_KODE(), outputEnhet.get().getAgressoId());
        assertEquals(mockedEnhet1.getENHETTYPE_NAVN(), outputEnhet.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet1.getORGNODE_NIV(), outputEnhet.get().getOrgNiv());

        var outputEnhet2 = orgenhetService.getEnhetByAgressoIdNivaAndName(
            mockedEnhet2.getORGENHET_KODE(),
            mockedEnhet2.getORGNODE_NIV(),
            mockedEnhet2.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet2.isPresent());

        assertEquals(mockedEnhet2.getORGENHET_KODE(), outputEnhet2.get().getAgressoId());
        assertEquals(mockedEnhet2.getENHETTYPE_NAVN(), outputEnhet2.get().getAgressoOrgenhetType());
        assertEquals(mockedEnhet2.getORGNODE_NIV(), outputEnhet2.get().getOrgNiv());

        var outputEnhet3 = orgenhetService.getEnhetByAgressoIdNivaAndName(
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

        verify(orgEnhetCommandService, times(3)).opprettOrgEnhet(any());

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        // This testing should be split up
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202204", null));
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet2, 2, "202204", null));
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet3, 3, "202204", null));

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

        verify(orgEnhetCommandService, times(3)).opprettOrgEnhet(any());

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        // This testing should be split up
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202204", null));
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet2, 2, "202204", null));
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet3, 3, "202204", null));

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

        verify(orgEnhetCommandService, times(1)).opprettOrgEnhet(any());

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        // This testing should be split up
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202108", null));

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
        assertNull(outputEnhet.get().getGyldigTom());
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

        verify(orgEnhetCommandService, times(3)).opprettOrgEnhet(any());
        verify(orgEnhetCommandService, times(2)).avvikleOrgEnhet(any(), any());

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        // This testing should be split up
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202108", "202112"));
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 2, "202202", "202204"));
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 3, "202206", null));

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
        assertNull(outputEnhet.get().getGyldigTom());
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

        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        // This testing should be split up
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202108", null));

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
        assertNull(outputEnhet.get().getGyldigTom());

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

        verify(orgEnhetCommandService, times(1)).opprettOrgEnhet(any());
        verify(orgEnhetCommandService, times(1)).avvikleOrgEnhet(any(), any());

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        // This testing should be split up
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202108", "202201"));

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
        assertNotNull(outputEnhet2.get().getGyldigTom());
        assertEquals(LocalDate.parse("2022-01-31"), outputEnhet2.get().getGyldigTom());
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

        verify(orgEnhetCommandService, times(2)).opprettOrgEnhet(any());
        verify(orgEnhetCommandService, times(1)).avvikleOrgEnhet(any(), any());

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        // This testing should be split up
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202008", "202012"));
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 2, "202106", null));

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
        assertNull(outputEnhet1.get().getGyldigTom());

        val historicFid = allEnheter.stream().filter(e -> !e.getFid().equals(outputEnhet1.get().getFid())).findFirst().orElseThrow().getFid();

        var historicOutputEnhet = orgenhetService.getByFid(historicFid);
        assertTrue(historicOutputEnhet.isPresent());
        assertEquals(LocalDate.parse("2020-12-31"), historicOutputEnhet.get().getGyldigTom());

        dvhOrgenhetMockUtils.createEmptyResponse();
        mockedEnhet1.setORGENHET_TIL_PERIODE(202102);
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        verify(orgEnhetCommandService, times(2)).opprettOrgEnhet(any());
        verify(orgEnhetCommandService, times(1)).avvikleOrgEnhet(any(), any());
        verify(orgEnhetCommandService, times(1)).korrigerOrgEnhet(any(), any());

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        // This testing should be split up
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202008", "202102"));

        allEnheter = orgenhetService.getAllEnheter();
        assertEquals(2, allEnheter.size());

        var outputEnhet2 = orgenhetService.getEnhetByAgressoIdNivaAndName(
            mockedEnhet1.getORGENHET_KODE(),
            mockedEnhet1.getORGNODE_NIV(),
            mockedEnhet1.getORGENHET_NAVN()
        );

        var historicStateFid = allEnheter.stream().filter(e -> !e.getFid().equals(outputEnhet2.orElseThrow().getFid())).findFirst().orElseThrow().getFid();

        historicOutputEnhet = orgenhetService.getByFid(historicStateFid);
        assertTrue(historicOutputEnhet.isPresent());
        assertEquals(LocalDate.parse("2021-02-28"), historicOutputEnhet.get().getGyldigTom());
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

        // DvhEnheter er slått samme der det er overlapp
        // Prossesering av dvhEnheter er seneste start dato først (start dato decend)

        // Mock1+2 er slått sammen i parsing.
        // Lager 'FID_1' [2020.08 - ]
        verify(orgEnhetCommandService, times(1)).opprettOrgEnhet(any());
        verify(orgEnhetCommandService, times(0)).avvikleOrgEnhet(any(), any());


        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        // This testing should be split up
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202008", null));

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
        assertNull(outputEnhet1.get().getGyldigTom());

        mockedEnhet1.setORGENHET_TIL_PERIODE(202011);
        dvhOrgenhetMockUtils.createEmptyResponse();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);

        when(orgEnhetCommandService.opprettOrgEnhet(argThat(arg -> arg.gyldigFom().equals(LocalDate.of(2020,8,1))))).thenReturn("FID_1");

        dvhRestConsumer.loadRessurserOgOrgEnheter();

        val outputEnhet1Before = orgenhetService.getEnhetByAgressoIdNivaAndName(
            mockedEnhet1.getORGENHET_KODE(),
            mockedEnhet1.getORGNODE_NIV(),
            mockedEnhet1.getORGENHET_NAVN()
        );

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
        verify(orgEnhetCommandService, times(1)).avvikleOrgEnhet(argThat(arg -> arg.equals("FID_1")), argThat(arg -> arg.gyldigTom().equals(LocalDate.of(2020,11,30))));
        verify(orgEnhetCommandService, times(1)).korrigerOrgEnhet(any(), any());

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        // This testing should be split up
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202101", null));
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet2, 2, "202008", "202011"));

        allEnheter = orgenhetService.getAllEnheter();
        assertEquals(2, allEnheter.size());

        val outputEnhet1After = orgenhetService.getEnhetByAgressoIdNivaAndName(
            mockedEnhet1.getORGENHET_KODE(),
            mockedEnhet1.getORGNODE_NIV(),
            mockedEnhet1.getORGENHET_NAVN()
        );
        assertTrue(outputEnhet1After.isPresent());

        assertEquals(outputEnhet1Before.get().getFid(), outputEnhet1After.get().getFid());
        assertEquals(LocalDate.parse("2021-01-01"), outputEnhet1After.get().getGyldigFom());
        assertNull(outputEnhet1After.get().getGyldigTom());

        val outputEnhet3 = allEnheter.stream().filter(e -> !e.getFid().equals(outputEnhet1After.get().getFid())).findAny();
        assertTrue(outputEnhet3.isPresent());
        assertEquals(LocalDate.parse("2020-08-01"), outputEnhet3.get().getGyldigFom());
        assertEquals(LocalDate.parse("2020-11-30"), outputEnhet3.get().getGyldigTom());
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

        verify(orgEnhetCommandService, times(2)).opprettOrgEnhet(any());
        verify(orgEnhetCommandService, times(1)).avvikleOrgEnhet(any(), any());

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        // This testing should be split up
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202008", "202011"));
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 2, "202101", null));


        var allEnheter = orgenhetService.getAllEnheter();
        assertEquals(2, allEnheter.size());

        mockedEnhet1.setORGENHET_TIL_PERIODE(202012);
        dvhOrgenhetMockUtils.createEmptyResponse();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        verify(orgEnhetCommandService, times(2)).opprettOrgEnhet(any());
        verify(orgEnhetCommandService, times(1)).avvikleOrgEnhet(any(), any());

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        // This testing should be split up
        // No changes!

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
        assertNull(outputEnhet1.get().getGyldigTom());

        val outputEnhet3 = allEnheter.stream().filter(e -> !e.getFid().equals(outputEnhet1.get().getFid())).findAny();
        assertTrue(outputEnhet3.isPresent());
        assertEquals(LocalDate.parse("2020-08-01"), outputEnhet3.get().getGyldigFom());
        assertEquals(LocalDate.parse("2020-11-30"), outputEnhet3.get().getGyldigTom());
    }

    @Test
    @SneakyThrows
    @DisplayName("Parkert enhet: Persistert enhet har ikke gyldigTOM, ny last så har enheten StillingMaks gyldig til dato")
    void scenario35() {
        OffsetDateTime stillingMaxTil = DvhOrgEnhetMock.getOffsetDateTime(2022,7,26);
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("TYPEEE")
            .ORGENHET_STATUS("P")
            .ORGENHET_FRA_PERIODE(202008)
            .ORGENHET_TIL_PERIODE(209912)
            .STILLING_MAKS_GYLDIG_TIL_DATO(stillingMaxTil)
            .build();

        orgEnhetStateRepository.update(
            dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, "202008", null));


        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        verify(orgEnhetCommandService, times(0)).opprettOrgEnhet(any());
        verify(orgEnhetCommandService, times(1)).avvikleOrgEnhet(any(), avvikleOrgEnhetCaptor.capture());

        assertEquals(localDateOf(stillingMaxTil), avvikleOrgEnhetCaptor.getValue().gyldigTom());
    }

    @Test
    @SneakyThrows
    @DisplayName("Parkert enhet: Persistert enhet har ikke gyldigTOM, ny last så har enheten StillingMaks gyldig til dato. Dvh har dette som to splittede enheter")
    void scenario36() {
        OffsetDateTime stillingMaxTil = DvhOrgEnhetMock.getOffsetDateTime(2022,7,26);
        val mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("TYPEEE")
            .ORGENHET_STATUS("P")
            .ORGENHET_FRA_PERIODE(200701)
            .ORGENHET_TIL_PERIODE(201805)
            .STILLING_MAKS_GYLDIG_TIL_DATO(stillingMaxTil)
            .build();
        val mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId")
            .ORGENHET_NAVN("testNavn")
            .ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("TYPEEE")
            .ORGENHET_STATUS("P")
            .ORGENHET_FRA_PERIODE(201806)
            .ORGENHET_TIL_PERIODE(209912)
            .STILLING_MAKS_GYLDIG_TIL_DATO(stillingMaxTil)
            .build();

        orgEnhetStateRepository.update(
            dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet2, 1, "201806", null));


        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        verify(orgEnhetCommandService, times(0)).opprettOrgEnhet(any());
        verify(orgEnhetCommandService, times(1)).avvikleOrgEnhet(any(), avvikleOrgEnhetCaptor.capture());

        assertEquals(localDateOf(stillingMaxTil), avvikleOrgEnhetCaptor.getValue().gyldigTom());
    }

    public static LocalDate localDateOf(OffsetDateTime date) {
        return LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
