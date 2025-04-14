package no.nav.nom.apps.api.ressurs.api.admin;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest(classes = {MastreStatligeINom.class})
class MastreStatligeINomTest {
    @MockitoBean
    RessursStateRepository ressursStateRepository;
    @MockitoBean
    OrgtilknytningService orgtilknytningService;
    @MockitoBean
    NomMastered nomMastered;
    @Autowired
    MastreStatligeINom mastreStatligeINom;

    @Test
    @DisplayName("Test ekskludering av navidenter ved mastring")
    void test_eksludering_av_navidenter() {
        when(ressursStateRepository.getAll()).thenReturn(List.of(
            getRessursState("A000001", Sektor.NAV_STATLIG),
            getRessursState("A000002", Sektor.NAV_STATLIG),
            getRessursState("A000003", Sektor.NAV_KOMMUNAL),
            getRessursState("A000004", Sektor.NAV_STATLIG),
            getRessursState("A000005", Sektor.EKSTERN),
            getRessursState("A000006", Sektor.NAV_STATLIG),
            getRessursState("A000007", Sektor.NAV_STATLIG)
        ));

        when(nomMastered.isMastered(any())).thenReturn(Boolean.FALSE);

        var res = mastreStatligeINom.markerStatligeSomMastret(true, List.of("A000001", "A000006"));

        verify(nomMastered, times(5)).isMastered(any());
        verify(nomMastered, times(1)).markRessursAsMasteredInNom("A000002");
        verify(nomMastered, times(1)).markRessursAsMasteredInNom("A000004");
        verify(nomMastered, times(1)).markRessursAsMasteredInNom("A000007");

        log.info(res);
    }

    @Test
    @DisplayName("Test ekskludering av navidenter ved mastring gjennom controller metode")
    void test_eksludering_av_navidenter_gjennom_controller_metode() {
        when(ressursStateRepository.getAll()).thenReturn(List.of(
            getRessursState("A000001", Sektor.NAV_STATLIG),
            getRessursState("A000002", Sektor.NAV_STATLIG),
            getRessursState("A000003", Sektor.NAV_KOMMUNAL),
            getRessursState("A000004", Sektor.NAV_STATLIG),
            getRessursState("A000005", Sektor.EKSTERN),
            getRessursState("A000006", Sektor.NAV_STATLIG),
            getRessursState("A000007", Sektor.NAV_STATLIG)
        ));

        when(nomMastered.isMastered(any())).thenReturn(Boolean.FALSE);

        var res = mastreStatligeINom.markerStatligeSomMastret("Confirm", List.of("A000001", "A000006"));

        verify(nomMastered, times(5)).isMastered(any());
        verify(nomMastered, times(1)).markRessursAsMasteredInNom("A000002");
        verify(nomMastered, times(1)).markRessursAsMasteredInNom("A000004");
        verify(nomMastered, times(1)).markRessursAsMasteredInNom("A000007");

        log.info(res.toString());
    }


    @Test
    @DisplayName("Test commit = false")
    void test_commit_false() {
        when(ressursStateRepository.getAll()).thenReturn(List.of(
            getRessursState("A000001", Sektor.NAV_STATLIG),
            getRessursState("A000002", Sektor.NAV_STATLIG),
            getRessursState("A000003", Sektor.NAV_KOMMUNAL),
            getRessursState("A000004", Sektor.NAV_STATLIG),
            getRessursState("A000005", Sektor.EKSTERN),
            getRessursState("A000006", Sektor.NAV_STATLIG),
            getRessursState("A000007", Sektor.NAV_STATLIG)
        ));

        when(nomMastered.isMastered(any())).thenReturn(Boolean.FALSE);

        var res = mastreStatligeINom.markerStatligeSomMastret(false, List.of("A000001", "A000006"));

        verify(nomMastered, times(5)).isMastered(any());
        verify(nomMastered, times(0)).markRessursAsMasteredInNom("A000002");
        verify(nomMastered, times(0)).markRessursAsMasteredInNom("A000004");
        verify(nomMastered, times(0)).markRessursAsMasteredInNom("A000007");

        log.info(res);
    }


    @Test
    @DisplayName("Test getRessursRow output")
    void getRessursRow() {
        var list = List.of(
            new MastreStatligeINom.Ressurs("123456789", Sektor.NAV_STATLIG, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"), LocalDate.parse("2024-01-01")),
            new MastreStatligeINom.Ressurs("123456789", Sektor.NAV_STATLIG, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"), LocalDate.parse("2024-01-01")),
            new MastreStatligeINom.Ressurs("123456789", Sektor.NAV_STATLIG, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"), LocalDate.parse("2024-01-01"))
        );

        StringBuffer sb = new StringBuffer();
        list.forEach(MastreStatligeINom.getRessursRow(sb));

        log.info("Resultat: {}", sb);
        assertEquals("123456789, NAV_STATLIG, 2023-01-01, 2023-12-31, 2024-01-01\n123456789, NAV_STATLIG, 2023-01-01, 2023-12-31, 2024-01-01\n123456789, NAV_STATLIG, 2023-01-01, 2023-12-31, 2024-01-01\n", sb.toString());
    }

    private RessursState getRessursState(String navident, Sektor sektor) {
        return RessursState.builder()
            .navident(navident)
            .sektor(sektor)
            .build();
    }
}