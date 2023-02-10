package no.nav.nom.apps.navident.job;

import lombok.SneakyThrows;
import no.nav.nom.apps.navident.SpringBootTestBase;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.eventsource.EventSourceService;
import no.nav.nom.apps.navident.eventsource.SourceEvent;
import no.nav.nom.apps.navident.eventsource.SourceEventRepository;
import org.junit.Ignore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportNavIdenterTest extends SpringBootTestBase {

    @Autowired
    ImportNavIdenter importNavIdenter;

    @MockBean
    SourceEventRepository sourceEventRepository;

    @MockBean
    KafkaTemplate<String, SourceEvent> producer;

    @MockBean
    EventSourceService eventSourceService;

    @Captor
    ArgumentCaptor<SourceEvent> eventCaptor;

    @Test()
    @SneakyThrows
    @DisplayName("Importer single linje - ident som ikke finnes")
    void importLineNewOk() {
        BufferedReader br = new BufferedReader(new StringReader(
            "A861330,01010100002,DAA0501,Demokratisk,Abakus,0501,NAV LILLEHAMMER-GAUSDAL,Aktiv,NAV Statlig,,,02.01.2023 14:57:00,01.02.2023,"
        ));

        importNavIdenter.importFromReader(br);

        verify(producer, times(1)).send(any(), eventCaptor.capture());
        verify(sourceEventRepository, times(1)).save(any());

        assertEquals( "A861330", eventCaptor.getValue().getPayload().getNavident());
        assertEquals( "01010100002", eventCaptor.getValue().getPayload().getPersonid());
        assertEquals(IdentType.STANDARD, eventCaptor.getValue().getPayload().getIdentType());
    }

    @Test()
    @SneakyThrows
    @DisplayName("Importer single linje - ident som allerde finnes")
    void importLineNewAlreadyExsists() {
        BufferedReader br = new BufferedReader(new StringReader(
            "A861330,01010100002,DAA0501,Demokratisk,Abakus,0501,NAV LILLEHAMMER-GAUSDAL,Aktiv,NAV Statlig,,,02.01.2023 14:57:00,01.02.2023,"
        ));

        when(eventSourceService.finnesNavIdent(any())).thenReturn(true);
        String res = importNavIdenter.importFromReader(br);

        verify(producer, times(0)).send(any(), any());
        verify(sourceEventRepository, times(0)).save(any());

        assertTrue(res.startsWith("Totalt importert 0 av 1"));
    }

    @Test()
    @SneakyThrows
    @DisplayName("Importer single linje -  header linjen")
    void importLineHeader() {
        BufferedReader br = new BufferedReader(new StringReader(
            "NAV-ID,Person id,Bruker id,Fornavn,Etternavn,Enhets-ID,Enhet,Status,Sektor,I permisjon?,I permisjon dato,Reg. dato,Tiltredelsesdato,Status-History.Slettet.TIME"
        ));

        String res = importNavIdenter.importFromReader(br);

        verify(producer, times(0)).send(any(), any());
        verify(sourceEventRepository, times(0)).save(any());

        assertTrue(res.startsWith("Totalt importert 0 av 1"));
    }

    @Test()
    @SneakyThrows
    @DisplayName("Importer flere linjer med header")
    void importMultipleLinesWithHeaderOK() {
        StringBuffer sb = new StringBuffer();
        BufferedReader br = new BufferedReader(new StringReader(
            """            
NAV-ID,Person id,Bruker id,Fornavn,Etternavn,Enhets-ID,Enhet,Status,Sektor,I permisjon?,I permisjon dato,Reg. dato,Tiltredelsesdato,Status-History.Slettet.TIME
A861330,01010100002,DAA0501,Demokratisk,Abakus,0501,NAV LILLEHAMMER-GAUSDAL,Aktiv,NAV Statlig,,,02.01.2023 14:57:00,01.02.2023,
A874750,01010100003,EAE0314,Engasjert,Abakus,0314,NAV SAGENE,Aktiv,NAV Statlig,,,09.01.2023 16:00:00,08.02.2023,
A866029,01010100004,EAA2820,Etterpåklok,Abakus,2820,HR-AVDELINGEN,Aktiv,Eksternt,,,06.01.2023 13:10:55,05.02.2023,
A851574,01010100005,GAB1130,Gretten,Abakus,1130,NAV STRAND,Aktiv,NAV Statlig,,,14.12.2022 15:18:00,,
A867804,01010100006,HAE4403,Horisontal,Abakus,4403,NAV ARBEID OG YTELSER KRISTIANIA,Aktiv,NAV Statlig,,,06.01.2023 16:07:00,05.02.2023,
            """
        ));

        String res = importNavIdenter.importFromReader(br);

        verify(producer, times(5)).send(any(), any());
        verify(sourceEventRepository, times(5)).save(any());

        assertTrue(res.startsWith("Totalt importert 5 av 6"), res);
    }

    @Test
    @Ignore
    @SneakyThrows
    @DisplayName("Manual import of local file. ")
    void importManuallyLocalFile() {
        Path path = Paths.get("/Users/g155033/Documents/atom/Remedy_preprod_20230207.csv");

        BufferedReader br = Files.newBufferedReader(path);
        String res = importNavIdenter.importFromReader(br);

        verify(producer, times(25107)).send(any(), any());
        verify(sourceEventRepository, times(25107)).save(any());

        assertTrue(res.startsWith("Totalt importert 25107 av 124461"), res);
    }

}