package no.nav.nom.apps.api.orgtilknytning.job;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.MasseflyttOrgtilknytningerService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.nom.contract.nom.Sektor.NAV_STATLIG;
import static no.nav.nom.contract.nom.Tilknytningtype.FAST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {OTUtenforHierarki.class})
class OTUtenforHierarkiTest {
    @MockitoBean
    MasseflyttOrgtilknytningerService masseflyttOrgtilknytningerService;
    @MockitoBean
    OrgtilknytningService orgtilknytningService;
    @MockitoBean
    OrganiseringStateRepository organiseringStateRepository;
    @MockitoBean
    OrgenhetService orgenhetService;
    @MockitoBean
    Unleash unleash;

    @Autowired
    OTUtenforHierarki otUtenforHierarki;

    @Captor
    ArgumentCaptor<List<String>> otFidInputCaptor;
    @Captor
    ArgumentCaptor<String> nyOrgenhetIdInputCaptor;
    @Captor
    ArgumentCaptor<Boolean> beholdTilgangerInputCaptor;
    @Captor
    ArgumentCaptor<LocalDate> gyldigFomInputCaptor;
    @Captor
    ArgumentCaptor<InputMetadata> inputMetadataInputCaptor;

    @BeforeEach
    void beforeEach() {
        when(unleash.isEnabled(any())).thenReturn(true);
    }

    @Test
    @DisplayName("Test på basic ok autoflytt")
    void test_basic_ok_autoflytt() {
        setupOrganisering();

        var ot1 = new OrgtilknytningState("FID_1", "A000001", enhet1.getId(), NAV_STATLIG, true, LocalDate.now().minusYears(1), null, FAST, null, null);

        HashMap<IOrgenhet, List<OrgtilknytningState>> perAlternativOrgenhet = new HashMap<>();
        perAlternativOrgenhet.put(enhet2, List.of(ot1));

        var feilede = otUtenforHierarki.autoflyttOTUtenforHierarki(perAlternativOrgenhet);

        verify(masseflyttOrgtilknytningerService, times(1)).flyttOrgtilknytninger(otFidInputCaptor.capture(),
            nyOrgenhetIdInputCaptor.capture(), beholdTilgangerInputCaptor.capture(), gyldigFomInputCaptor.capture(), inputMetadataInputCaptor.capture(), eq(true));

        assertEquals(0, feilede);
        assertEquals(List.of("FID_1"), otFidInputCaptor.getValue());
        assertEquals(enhet2.getId(), nyOrgenhetIdInputCaptor.getValue());
        assertFalse(beholdTilgangerInputCaptor.getValue());
        assertEquals(today, gyldigFomInputCaptor.getValue());
    }

    @Test
    @DisplayName("Test på autoflytt feil på organisering")
    void test_autoflytt_feil_paa_organisering() {
        setupOrganisering();

        var ot1 = new OrgtilknytningState("FID_1", "A000001", enhet1.getId(), NAV_STATLIG, true, LocalDate.now().minusYears(1), null, FAST, null, null);

        HashMap<IOrgenhet, List<OrgtilknytningState>> perAlternativOrgenhet = new HashMap<>();
        perAlternativOrgenhet.put(enhet3, List.of(ot1));

        var feilede = otUtenforHierarki.autoflyttOTUtenforHierarki(perAlternativOrgenhet);

        assertEquals(1, feilede);
    }

    @Test
    @DisplayName("Test på autoflytt feil på flyttOrgtilknytninger kall")
    void test_autoflytt_feil_paa_flyttOrgtilknytninger_kall() {
        setupOrganisering();

        var ot1 = new OrgtilknytningState("FID_1", "A000001", enhet1.getId(), NAV_STATLIG, true, LocalDate.now().minusYears(1), null, FAST, null, null);

        HashMap<IOrgenhet, List<OrgtilknytningState>> perAlternativOrgenhet = new HashMap<>();
        perAlternativOrgenhet.put(enhet1, List.of(ot1));

        when(masseflyttOrgtilknytningerService.flyttOrgtilknytninger(any(), any(), anyBoolean(), any(), any(), anyBoolean()))
            .thenReturn(new HashMap<>(Map.of(ot1, "Feil")));

        var feilede = otUtenforHierarki.autoflyttOTUtenforHierarki(perAlternativOrgenhet);

        assertEquals(1, feilede);
    }


    @Test
    @DisplayName("Test på basic ok finn ot utenfor hierarki")
    void test_basic_ok_finn_ot_utenfor_hierarki() {
        when(orgenhetService.getAllEnheterMedOrganisering()).thenReturn(List.of(enhet2, enhet3));
        when(orgenhetService.getById(enhet1.getId())).thenReturn(Optional.of(enhet1));
        setupOrganisering();

        var ot1 = new OrgtilknytningState("FID_1", "A000001", enhet1.getId(), NAV_STATLIG, true, LocalDate.now().minusYears(1), null, FAST, null, null);
        var ot2 = new OrgtilknytningState("FID_2", "A000002", enhet2.getId(), NAV_STATLIG, true, LocalDate.now().minusYears(1), null, FAST, null, null);

        when(orgtilknytningService.getAll()).thenReturn(List.of(ot1, ot2));

        otUtenforHierarki.findOTUtenforHierarkiFromDataLast();

        verify(masseflyttOrgtilknytningerService, times(1)).flyttOrgtilknytninger(otFidInputCaptor.capture(),
            nyOrgenhetIdInputCaptor.capture(), beholdTilgangerInputCaptor.capture(), gyldigFomInputCaptor.capture(), inputMetadataInputCaptor.capture(), eq(true));

        assertEquals(List.of("FID_1"), otFidInputCaptor.getValue());
        assertEquals(enhet2.getId(), nyOrgenhetIdInputCaptor.getValue());
        assertFalse(beholdTilgangerInputCaptor.getValue());
        assertEquals(today, gyldigFomInputCaptor.getValue());
    }

    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);

    private void setupOrganisering() {
        var organisering1 = new OrganiseringState("FID_1", enhet1.getId(), enhet1.getNavn(), enhet3.getId(), enhet3.getNavn(), LocalDate.now().minusYears(3), yesterday);
        var organisering2 = new OrganiseringState("FID_2", enhet2.getId(), enhet2.getNavn(), enhet3.getId(), enhet3.getNavn(), today, null);

        when(organiseringStateRepository.findActiveById(enhet1.getId())).thenReturn(Optional.of(organisering1));
        when(organiseringStateRepository.findActiveById(enhet2.getId())).thenReturn(Optional.of(organisering2));
    }


    IOrgenhet enhet1 = new OrgEnhet(
        OrgEnhetState.builder()
            .fid("FID_1")
            .id("a123bc")
            .navn("Enhet 1 - Gammel")
            .agressoId("AG1")
            .agressoOrgNivaa("ORGENHET")
            .gyldigFom(LocalDate.now().minusYears(3))
            .gyldigTom(null)
            .build()
    );

    IOrgenhet enhet2 = new OrgEnhet(
        OrgEnhetState.builder()
            .fid("FID_2")
            .id("ef456g")
            .navn("Enhet 2 - Ny")
            .agressoId("AG1")
            .agressoOrgNivaa("ORGENHET")
            .gyldigFom(LocalDate.now().minusYears(3))
            .gyldigTom(null)
            .build()
    );

    IOrgenhet enhet3 = new OrgEnhet(
        OrgEnhetState.builder()
            .fid("FID_3")
            .id("xy890z")
            .navn("Enhet 3")
            .agressoId("AG3")
            .agressoOrgNivaa("ORGNIV_3")
            .gyldigFom(LocalDate.now().minusYears(3))
            .gyldigTom(null)
            .build()
    );
}