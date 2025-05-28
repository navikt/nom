package no.nav.nom.apps.api.ressurs.api.admin;

import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MigreringVedLanseringMastringKommunaleOgEksterneTest {

    @Mock
    RessursStateRepository ressursStateRepository;
    @Mock
    NomMastered nomMastered;

    List<RessursState> ressurser = testRessurser();

    MigreringVedLanseringMastringKommunaleOgEksterne CUT;

    private static List<RessursState> testRessurser() {
        return List.of(
            getRessursState("FID_1", "A000001", Sektor.NAV_STATLIG),
            getRessursState("FID_1", "A000002", Sektor.NAV_STATLIG),
            getRessursState("FID_1", "A000003", Sektor.NAV_KOMMUNAL),
            getRessursState("FID_1", "A000004", Sektor.NAV_KOMMUNAL),
            getRessursState("FID_1", "A000005", Sektor.NAV_KOMMUNAL),
            getRessursState("FID_1", "A000006", Sektor.EKSTERN),
            getRessursState("FID_1", "A000007", Sektor.NAV_STATLIG),
            getRessursState("FID_1", "A000008", Sektor.NAV_KOMMUNAL)
        );
    }

    @NotNull
    private static RessursState getRessursState(String fid, String navident, Sektor sektor) {
        return new RessursState(
            fid,
            "123",
            navident,
            sektor,
            new Visningsnavn("Test", Optional.empty(), "Test"),
            null,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Collections.EMPTY_LIST,
            IdentType.STANDARD,
            "S123456"
        );
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        CUT = new MigreringVedLanseringMastringKommunaleOgEksterne(ressursStateRepository, nomMastered);
    }

    @Test
    void markerKommunaleSomMastret() {
        when(ressursStateRepository.getAll()).thenReturn(ressurser);
        when(nomMastered.isMastered("A000001")).thenReturn(true);
        when(nomMastered.isMastered("A000008")).thenReturn(true);

        String result = CUT.markerKommunaleSomMastret(false);
        assertEquals("3 kommunale kan markeres som mastret", result);
    }

    @Test
    void markerKommunaleSomMastret_2() {
        when(ressursStateRepository.getAll()).thenReturn(ressurser);
        when(nomMastered.isMastered("A000001")).thenReturn(true);
        when(nomMastered.isMastered("A000008")).thenReturn(true);

        String result = CUT.markerKommunaleSomMastret(true);

        verify(nomMastered, times(3)).markRessursAsMasteredInNom(anyString());
        assertEquals("3 kommunale er nå markert som mastret", result);
    }


}