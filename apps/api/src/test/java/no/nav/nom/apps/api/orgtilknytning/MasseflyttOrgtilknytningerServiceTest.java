package no.nav.nom.apps.api.orgtilknytning;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.api.email.OrgtilknytningEndretEmailService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest(classes = {MasseflyttOrgtilknytningerService.class})
class MasseflyttOrgtilknytningerServiceTest {
    LocalDate today = LocalDate.now();

    @MockitoBean
    OrgtilknytningStateRepository orgtilknytningStateRepository;
    @MockitoBean
    OrgenhetService orgenhetService;
    @MockitoBean
    OrgtilknytningCommandService orgtilknytningCommandService;
    @MockitoBean
    NomMastered nomMastered;
    @MockitoBean
    OrgtilknytningEndretEmailService orgtilknytningEndretEmailService;
    @MockitoBean
    OrgtilknytningService orgtilknytningService;
    @MockitoBean
    NomStateOrgtilknytningListener nomStateOrgtilknytningListener;

    @Autowired
    MasseflyttOrgtilknytningerService cut;

    @Captor
    ArgumentCaptor<OpprettOrgtilknytning> opprettOrgtilknytningArgumentCaptor;

    @Captor
    ArgumentCaptor<AvvikleOrgtilknytning> avvikleOrgtilknytningCommandArgumentCaptor;
    private int otFidCounter = 1;
    private int orgenhetFidCounter = 1;

    @Test
    @DisplayName("Alle orgtilknytninger er aktive på flyttedato")
    void flyttOrgtilknytninger_alle_OTer_aktive_paa_flyttedato() {
        boolean beholdTilganger = true;
        OrgEnhet nyOrgenhet = getOrgenhet("ab123c", "Ny Enhet");
        OrgEnhet gmlOrgenhet = getOrgenhet("de456f", "Gml Enhet");

        List<OrgtilknytningState> orgtilknytninger = List.of(
            getOT("A000001", gmlOrgenhet.getId()),
            getOT("A000002", gmlOrgenhet.getId()),
            getOT("A000003", gmlOrgenhet.getId())
        );

        when(orgtilknytningStateRepository.findByFidIn(any())).thenReturn(orgtilknytninger);
        when(orgenhetService.getById(any())).thenReturn(Optional.of(nyOrgenhet));

        cut.flyttOrgtilknytninger(orgtilknytninger.stream().map(OrgtilknytningState::fid).toList(), nyOrgenhet.getId(), beholdTilganger, today, null);

        verify(orgtilknytningCommandService, times(3)).avvikleOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(0)).fjernOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(3)).endreDagligOppfolgingOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(3)).opprettOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(3)).endreTilknytningtype(any());
    }

    @Test
    @DisplayName("Orgtilknytning starter på flyttedato")
    void flyttOrgtilknytninger_OT_starter_paa_flyttedato() {
        boolean beholdTilganger = true;
        OrgEnhet nyOrgenhet = getOrgenhet("ab123c", "Ny Enhet");
        OrgEnhet gmlOrgenhet = getOrgenhet("de456f", "Gml Enhet");

        var originalOT = getOT("A000001", gmlOrgenhet.getId(), today, null);
        List<OrgtilknytningState> orgtilknytninger = List.of(
            originalOT
        );

        when(orgtilknytningStateRepository.findByFidIn(any())).thenReturn(orgtilknytninger);
        when(orgenhetService.getById(any())).thenReturn(Optional.of(nyOrgenhet));

        cut.flyttOrgtilknytninger(orgtilknytninger.stream().map(OrgtilknytningState::fid).toList(), nyOrgenhet.getId(), beholdTilganger, today, null);

        verify(orgtilknytningCommandService, times(0)).avvikleOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(1)).fjernOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(1)).endreDagligOppfolgingOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(1)).opprettOrgtilknytning(opprettOrgtilknytningArgumentCaptor.capture());

        assertArgumentEquals(opprettOrgtilknytningArgumentCaptor.getValue(), originalOT, nyOrgenhet.getId(), beholdTilganger);
    }

    @Test
    @DisplayName("Orgtilknytning starter etter flyttedato")
    void flyttOrgtilknytninger_OT_starter_etter_flyttedato() {
        boolean beholdTilganger = true;
        OrgEnhet nyOrgenhet = getOrgenhet("ab123c", "Ny Enhet");
        OrgEnhet gmlOrgenhet = getOrgenhet("de456f", "Gml Enhet");

        var originalOT = getOT("A000001", gmlOrgenhet.getId(), today, null);
        List<OrgtilknytningState> orgtilknytninger = List.of(
            originalOT
        );

        when(orgtilknytningStateRepository.findByFidIn(any())).thenReturn(orgtilknytninger);
        when(orgenhetService.getById(any())).thenReturn(Optional.of(nyOrgenhet));

        cut.flyttOrgtilknytninger(orgtilknytninger.stream().map(OrgtilknytningState::fid).toList(), nyOrgenhet.getId(), beholdTilganger, today, null);

        verify(orgtilknytningCommandService, times(0)).avvikleOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(1)).fjernOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(1)).endreDagligOppfolgingOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(1)).opprettOrgtilknytning(opprettOrgtilknytningArgumentCaptor.capture());

        assertArgumentEquals(opprettOrgtilknytningArgumentCaptor.getValue(), originalOT, nyOrgenhet.getId(), beholdTilganger);
    }

    @Test
    @DisplayName("Orgtilknytning slutter før flyttedato")
    void flyttOrgtilknytninger_OT_slutter_foer_flyttedato() {
        boolean beholdTilganger = true;
        OrgEnhet nyOrgenhet = getOrgenhet("ab123c", "Ny Enhet");
        OrgEnhet gmlOrgenhet = getOrgenhet("de456f", "Gml Enhet");

        var originalOT = getOT("A000001", gmlOrgenhet.getId(), today.minusMonths(6), today.minusMonths(1));
        List<OrgtilknytningState> orgtilknytninger = List.of(
            originalOT
        );

        when(orgtilknytningStateRepository.findByFidIn(any())).thenReturn(orgtilknytninger);
        when(orgenhetService.getById(any())).thenReturn(Optional.of(nyOrgenhet));

        var res = cut.flyttOrgtilknytninger(orgtilknytninger.stream().map(OrgtilknytningState::fid).toList(), nyOrgenhet.getId(), beholdTilganger, today, null);

        verify(orgtilknytningCommandService, times(0)).avvikleOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(0)).fjernOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(0)).endreDagligOppfolgingOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(0)).opprettOrgtilknytning(any());

        assertEquals(1, res.size());
        assertEquals("Orgtilknytning er ikke gyldig for flytting på angitt flyttedato", res.get(orgtilknytninger.get(0)));
    }

    @Test
    @DisplayName("Orgtilknytning slutter på flyttedato")
    void flyttOrgtilknytninger_OT_slutter_på_flyttedato() {
        boolean beholdTilganger = true;
        OrgEnhet nyOrgenhet = getOrgenhet("ab123c", "Ny Enhet");
        OrgEnhet gmlOrgenhet = getOrgenhet("de456f", "Gml Enhet");

        var originalOT = getOT("A000001", gmlOrgenhet.getId(), today.minusMonths(6), today);
        List<OrgtilknytningState> orgtilknytninger = List.of(
            originalOT
        );

        when(orgtilknytningStateRepository.findByFidIn(any())).thenReturn(orgtilknytninger);
        when(orgenhetService.getById(any())).thenReturn(Optional.of(nyOrgenhet));

        var res = cut.flyttOrgtilknytninger(orgtilknytninger.stream().map(OrgtilknytningState::fid).toList(), nyOrgenhet.getId(), beholdTilganger, today, null);

        verify(orgtilknytningCommandService, times(0)).avvikleOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(0)).fjernOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(0)).endreDagligOppfolgingOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(0)).opprettOrgtilknytning(opprettOrgtilknytningArgumentCaptor.capture());

        assertEquals(1, res.size());
        assertEquals("Orgtilknytning er ikke gyldig for flytting på angitt flyttedato", res.get(orgtilknytninger.get(0)));
    }

    @Test
    @DisplayName("Orgtilknytninger med sluttdato")
    void orgtilknytninger_med_sluttdato() {
        boolean beholdTilganger = false;
        OrgEnhet nyOrgenhet = getOrgenhet("ab123c", "Ny Enhet", LocalDate.of(2022, 4, 1), null);
        OrgEnhet gmlOrgenhet = getOrgenhet("de456f", "Gml Enhet", LocalDate.of(2024, 10, 1), null);
        OrgEnhet gmlOrgenhet2 = getOrgenhet("xy890z", "Gml Enhet 2", LocalDate.of(2022, 2, 1), null);

        var originalOT = getOT(
            "A000001",
            gmlOrgenhet.getId(),
            Sektor.EKSTERN,
            Tilknytningtype.KONSULENT,
            true,
            LocalDate.of(2025, 4, 7),
            LocalDate.of(2025, 4, 30)
        );
        var originalOT2 = getOT(
            "A000001",
            gmlOrgenhet2.getId(),
            Sektor.EKSTERN,
            Tilknytningtype.KONSULENT,
            true,
            LocalDate.of(2025, 1, 22),
            LocalDate.of(2025, 10, 11)
        );
        List<OrgtilknytningState> orgtilknytninger = List.of(
            originalOT, originalOT2
        );

        when(orgtilknytningStateRepository.findByFidIn(any())).thenReturn(orgtilknytninger);
        when(orgenhetService.getById(any())).thenReturn(Optional.of(nyOrgenhet));

        var res = cut.flyttOrgtilknytninger(
            orgtilknytninger.stream().map(OrgtilknytningState::fid).toList(),
            nyOrgenhet.getId(),
            beholdTilganger,
            LocalDate.of(2025, 4, 9),
            null
        );

        verify(orgtilknytningCommandService, times(4)).avvikleOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(0)).fjernOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(2)).endreDagligOppfolgingOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(2)).endreTilknytningtype(any());
        verify(orgtilknytningCommandService, times(2)).opprettOrgtilknytning(opprettOrgtilknytningArgumentCaptor.capture());

        assertEquals(0, res.size());
    }

    @Test
    @DisplayName("Orgtilknytninger med sluttdato, en utenfor orgenhet periode")
    void orgtilknytninger_med_sluttdato_en_utenfor_orgenhet_periode() {
        boolean beholdTilganger = false;
        OrgEnhet nyOrgenhet = getOrgenhet("ab123c", "Ny Enhet", LocalDate.of(2022, 4, 1), LocalDate.of(2025, 10, 1));
        OrgEnhet gmlOrgenhet = getOrgenhet("de456f", "Gml Enhet", LocalDate.of(2024, 10, 1), null);
        OrgEnhet gmlOrgenhet2 = getOrgenhet("xy890z", "Gml Enhet 2", LocalDate.of(2022, 2, 1), null);

        var originalOT = getOT(
            "A000001",
            gmlOrgenhet.getId(),
            Sektor.EKSTERN,
            Tilknytningtype.KONSULENT,
            true,
            LocalDate.of(2025, 4, 7),
            LocalDate.of(2025, 4, 30)
        );
        var originalOT2 = getOT(
            "A000001",
            gmlOrgenhet2.getId(),
            Sektor.EKSTERN,
            Tilknytningtype.KONSULENT,
            true,
            LocalDate.of(2025, 1, 22),
            LocalDate.of(2025, 10, 11)
        );
        List<OrgtilknytningState> orgtilknytninger = List.of(
            originalOT, originalOT2
        );

        when(orgtilknytningStateRepository.findByFidIn(any())).thenReturn(orgtilknytninger);
        when(orgenhetService.getById(any())).thenReturn(Optional.of(nyOrgenhet));

        var res = cut.flyttOrgtilknytninger(
            orgtilknytninger.stream().map(OrgtilknytningState::fid).toList(),
            nyOrgenhet.getId(),
            beholdTilganger,
            LocalDate.of(2025, 4, 9),
            null
//            , true // Also test visitor and wait for state
        );

        verify(orgtilknytningCommandService, times(2)).avvikleOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(0)).fjernOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(1)).endreDagligOppfolgingOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(1)).endreTilknytningtype(any());
        verify(orgtilknytningCommandService, times(1)).opprettOrgtilknytning(opprettOrgtilknytningArgumentCaptor.capture());

        assertEquals(1, res.size());
        assertEquals("Orgtilknytning er gyldig etter ny orgenhet sin gyldighetsperiode", res.get(orgtilknytninger.get(1)));
    }

    private void assertArgumentEquals(OpprettOrgtilknytning actual, OrgtilknytningState originalOT, String nyOrgEnhetId, boolean beholdTilganger) {
        assertThat(actual).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(OpprettOrgtilknytning.builder()
                .navident(originalOT.navident())
                .orgenhetId(nyOrgEnhetId)
                .sektor(originalOT.sektor())
                .gyldigFom(today)
                .masseflyttetFraFid(beholdTilganger ? originalOT.fid() : null)
                .build()
            );
    }

    private OrgtilknytningState getOT(String navident, String orgenhetsId) {
        return getOT(navident, orgenhetsId, Sektor.NAV_STATLIG, Tilknytningtype.FAST, true, today.minusMonths(6), null);
    }

    private OrgtilknytningState getOT(String navident, String orgenhetsId, LocalDate gyldigFom, LocalDate gyldigTom) {
        return getOT(navident, orgenhetsId, Sektor.NAV_STATLIG, Tilknytningtype.FAST, true, gyldigFom, gyldigTom);
    }

    private OrgtilknytningState getOT(
        String navident,
        String orgenhetsId,
        Sektor sektor,
        Tilknytningtype tilknytningType,
        boolean erDagligOppfolging,
        LocalDate gyldigFom,
        LocalDate gyldigTom
    ) {
        return new OrgtilknytningState(
            "FID_" + otFidCounter++,
            navident,
            orgenhetsId,
            sektor,
            erDagligOppfolging,
            gyldigFom,
            gyldigTom == null ? Optional.empty() : Optional.of(gyldigTom),
            tilknytningType == null ? Optional.empty() : Optional.of(tilknytningType),
            Optional.empty(),
            "S123456"
        );
    }

    private OrgEnhet getOrgenhet(String id, String navn) {
        return getOrgenhet(id, navn, today.minusYears(1), null);
    }

    private OrgEnhet getOrgenhet(String id, String navn, LocalDate gyldigFom, LocalDate gyldigTom) {
        return new OrgEnhet(new OrgEnhetState(
            "FID_" + orgenhetFidCounter++,
            id,
            navn,
            gyldigFom,
            gyldigTom == null ? Optional.empty() : Optional.of(gyldigTom),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        ));
    }
}