package no.nav.nom.apps.navident.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.getunleash.Unleash;
import lombok.SneakyThrows;
import no.nav.nom.apps.navident.SpringBootTestBase;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.apps.navident.eventsource.EventSourceService;
import no.nav.nom.apps.navident.eventsource.SourceEvent;
import no.nav.nom.apps.navident.eventsource.SourceEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.stubbing.Answer;
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
import static org.mockito.ArgumentMatchers.anyString;
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

    @MockBean
    protected Unleash unleash;

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        // Always enable features for tests
        when(unleash.isEnabled(any())).thenReturn(true);
    }

    @Test()
    @SneakyThrows
    @DisplayName("Importer single linje - ident som ikke finnes")
    void importLineNewOk() {
        BufferedReader br = new BufferedReader(new StringReader(
            "A861330,DAA0501,Demokratisk,Abakus,19437702929,0501,NAV LILLEHAMMER-GAUSDAL,Aktiv,NAV Statlig,,,02.01.2023 14:57:00,01.02.2023,"
        ));

        importNavIdenter.importFromReader(br, false);

        verify(producer, times(1)).send(any(), eventCaptor.capture());
        verify(sourceEventRepository, times(1)).save(any());

        assertEquals( "A861330", eventCaptor.getValue().getPayload().getNavident());
        assertEquals( "19437702929", eventCaptor.getValue().getPayload().getPersonid());
        assertEquals(IdentType.STANDARD, eventCaptor.getValue().getPayload().getIdentType());
    }

    @Test()
    @SneakyThrows
    @DisplayName("Importer single linje - ident som allerde finnes")
    void importLineNewAlreadyExsists() {
        BufferedReader br = new BufferedReader(new StringReader(
            "A861330,DAA0501,Demokratisk,Abakus,19437702929,0501,NAV LILLEHAMMER-GAUSDAL,Aktiv,NAV Statlig,,,02.01.2023 14:57:00,01.02.2023,"
        ));

        when(eventSourceService.finnesNavIdent(any())).thenReturn(true);
        String res = importNavIdenter.importFromReader(br, false);

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

        String res = importNavIdenter.importFromReader(br, false);

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
NAV-ID,Bruker id,Fornavn,Etternavn,Fødselsnummer,Enhets-ID,Enhet,Status,Sektor,I permisjon?,I permisjon dato,Reg. dato,Tiltredelsesdato,Slettet dato
A861330,DAA0501,Demokratisk,Abakus,19437702929,0501,NAV LILLEHAMMER-GAUSDAL,Aktiv,NAV Statlig,,,02.01.2023 14:57:00,01.02.2023,
A874750,EAE0314,Engasjert,Abakus,02488948302,0314,NAV SAGENE,Aktiv,NAV Statlig,,,09.01.2023 16:00:00,08.02.2023,
A866029,EAA2820,Etterpåklok,Abakus,30528514967,2820,HR-AVDELINGEN,Aktiv,Eksternt,,,06.01.2023 13:10:55,05.02.2023,
A851574,GAB1130,Gretten,Abakus,22458345140,1130,NAV STRAND,Aktiv,NAV Statlig,,,14.12.2022 15:18:00,,
A867804,HAE4403,Horisontal,Abakus,27469208996,4403,NAV ARBEID OG YTELSER KRISTIANIA,Aktiv,NAV Statlig,,,06.01.2023 16:07:00,05.02.2023,
            """
        ));

        String res = importNavIdenter.importFromReader(br, false);

        verify(producer, times(5)).send(any(), any());
        verify(sourceEventRepository, times(5)).save(any());

        assertTrue(res.startsWith("Totalt importert 5 av 6"), res);
    }

    @Test
    @Disabled
    @SneakyThrows
    @DisplayName("Manual import of local file. ")
    void importManuallyLocalFile() {
        Path path = Paths.get("/Users/g155033/Documents/atom/Remedy_preprod_20230207.csv");

        BufferedReader br = Files.newBufferedReader(path);
        String res = importNavIdenter.importFromReader(br, false);

        verify(producer, times(25107)).send(any(), any());
        verify(sourceEventRepository, times(25107)).save(any());

        assertTrue(res.startsWith("Totalt importert 25107 av 124461"), res);
    }

    @Test
    @DisplayName("Test import all navident from a resource file. ")
    void importAllIdentTypeFromResourceFile () {
        when(eventSourceService.finnesNavIdent(any())).thenReturn(true);
        when(eventSourceService.getsNavIdent(anyString())).thenAnswer((Answer<Navident>) invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            //mocking Navident that already exists and with standard ident type
            return Navident.builder().navident((String) args[0]).identType(IdentType.STANDARD).build();
        });

        String res = importNavIdenter.importIdentTypeFromResourceFile();
        assertTrue(res.startsWith("Totalt ident importert fra fil 592 av 592."), res);

        verify(producer, times(592)).send(any(), any());
        verify(sourceEventRepository, times(592)).save(any());
    }

    @Test
    @DisplayName("Test do not import navident from a resource file. ")
    void importNoneIdentTypeFromResourceFile () {
        when(eventSourceService.finnesNavIdent(any())).thenReturn(false);
        String res = importNavIdenter.importIdentTypeFromResourceFile();
        assertTrue(res.startsWith("Totalt ident importert fra fil 0 av 592."), res);
        verify(producer, times(0)).send(any(), any());
        verify(sourceEventRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("Test import some navident from a resource file. ")
    void importSomeIdentTypeFromResourceFile () {
        when(eventSourceService.finnesNavIdent(any())).thenReturn(true);
        when(eventSourceService.getsNavIdent(anyString())).thenAnswer((Answer<Navident>) invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            //mocking such that Navident that starts with *11 as unknown ident type
            IdentType identType = (((String) args[0]).substring(1).startsWith("11"))? IdentType.UKJENT : IdentType.STANDARD;
            return Navident.builder().navident((String) args[0]).identType(identType).build();
        });

        String res = importNavIdenter.importIdentTypeFromResourceFile();
        assertTrue(res.startsWith("Totalt ident importert fra fil 589 av 592."), res);
        verify(producer, times(589)).send(any(), any());
        verify(sourceEventRepository, times(589)).save(any());
    }

    @Test
    @DisplayName("Test that navident is valid")
    void test_isValidNavIdentWithSequenceLessThan168000 () {
        String valid_navident_1 = "A123456";
        String invalid_navident_1 = "";
        String invalid_navident_2 = "123456";
        String valid_navident_but_sequence_higherthan_168000 = "A223456";
        assertTrue(importNavIdenter.isValidNavIdentWithSequenceLessThan168000(valid_navident_1));
        assertFalse(importNavIdenter.isValidNavIdentWithSequenceLessThan168000(invalid_navident_1));
        assertFalse(importNavIdenter.isValidNavIdentWithSequenceLessThan168000(invalid_navident_2));
        assertFalse(importNavIdenter.isValidNavIdentWithSequenceLessThan168000(valid_navident_but_sequence_higherthan_168000));
    }

}