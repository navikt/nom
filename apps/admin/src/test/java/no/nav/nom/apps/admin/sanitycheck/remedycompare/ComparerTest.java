package no.nav.nom.apps.admin.sanitycheck.remedycompare;

import no.nav.nom.apps.admin.webclient.nomapiclient.NomApiClient;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ComparerTest {

    @InjectMocks
    private Comparer comparer;

    @Mock
    private RemedyDataFileRepository remedyDataFileRepository;

    @Mock
    private RessursStateRepository ressursStateRepository;

    @Mock
    private MasteredRessursStateRepository masteredRessursStateRepository;

    @Mock
    private NomApiClient nomApiClient;

    @Captor
    private ArgumentCaptor<HashMap<String, RemedyCompareResult>> diffPaaRemedyEnhet;

    @Test
    @DisplayName("Test for å sammenligne NOM EnhetId mot Remedy EnhetId")
    void test_001() {
        var ressurser = List.of(
            new RessursState(
                "fid-001",
                "1234567812345",
                "S100000",
                Sektor.NAV_STATLIG,
                new Visningsnavn("Første Fornavn", Optional.empty(), "Etternavn"),
                new Jobbtelefon("12345678", TelefonType.NAV_TJENESTE_TELEFON),
                null,
                null,
                null,
                "2700",
                List.of(),
                IdentType.STANDARD,
                "X123456",
                null
            ),
            new RessursState(
                "fid-002",
                "1234567812346",
                "S100001",
                Sektor.NAV_STATLIG,
                new Visningsnavn("Andre Fornavn", Optional.empty(), "Etternavn"),
                new Jobbtelefon("87654321", TelefonType.NAV_TJENESTE_TELEFON),
                null,
                LocalDate.now().plusYears(1),
                LocalDate.now().minusYears(2),
                "2700",
                List.of(),
                IdentType.STANDARD,
                "X123456",
                null
            ),
            new RessursState(
                "fid-003",
                "1234567812347",
                "S100002",
                Sektor.NAV_STATLIG,
                new Visningsnavn("Tredje Fornavn", Optional.empty(), "Etternavn"),
                new Jobbtelefon("11223344", TelefonType.NAV_TJENESTE_TELEFON),
                null,
                LocalDate.now().minusYears(1).minusMonths(6),
                LocalDate.now().minusYears(2),
                "2700",
                List.of(),
                IdentType.STANDARD,
                "X123456",
                null
            )
        );

        var remedyUsers = List.of(
            new ImportedRemedyUser(
                "S100000",
                "Første Fornavn",
                "Etternavn",
                "2980",
                "Nav Teknologiavdeling",
                Sektor.NAV_STATLIG,
                true,
                false,
                null,
                LocalDate.now().minusYears(3),
                LocalDate.now().minusYears(3)
            ),
            new ImportedRemedyUser(
                "S100001",
                "Andre Fornavn",
                "Etternavn",
                "2980",
                "Nav Teknologiavdeling",
                Sektor.NAV_STATLIG,
                true,
                false,
                LocalDate.now().plusYears(1),
                LocalDate.now().minusYears(2),
                LocalDate.now().minusYears(2)
            ),
            new ImportedRemedyUser(
                "S100002",
                "Tredje Fornavn",
                "Etternavn",
                "2980",
                "Nav Teknologiavdeling",
                Sektor.NAV_STATLIG,
                false,
                false,
                LocalDate.now().minusYears(1).minusMonths(6),
                LocalDate.now().minusYears(2),
                LocalDate.now().minusYears(2)
            )
        );

        when(ressursStateRepository.getAll()).thenReturn(ressurser);
        when(remedyDataFileRepository.getRemedyUser(anyString())).thenReturn(remedyUsers.getFirst());
        when(remedyDataFileRepository.getRemedyUser(anyString())).thenReturn(remedyUsers.get(1));
        when(remedyDataFileRepository.getRemedyUser(anyString())).thenReturn(remedyUsers.getLast());
        when(nomApiClient.getRessursRemedyEnhet(remedyUsers.getFirst().navident())).thenReturn("2990");
        when(nomApiClient.getRessursRemedyEnhet(remedyUsers.get(1).navident())).thenReturn("2990");

        comparer.compareNomWithRemedy();

        Map<String, RemedyCompareResult> diff = comparer.getDiffPaaRemedyEnhet();
        assertEquals(2, diff.size());
    }
}
