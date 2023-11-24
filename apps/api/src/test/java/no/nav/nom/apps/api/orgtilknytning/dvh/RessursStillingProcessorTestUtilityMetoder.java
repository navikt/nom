package no.nav.nom.apps.api.orgtilknytning.dvh;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.ressurs.masterdata.NomMastered;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.NO_END_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@DisplayName("Tester Utility metoder for bruk i tolking og generering av events for RessursStillinger fra DVH")
class RessursStillingProcessorTestUtilityMetoder {

    @Mock
    OrgtilknytningCommandService orgtilknytningCommandService;

    @Mock
    OrgenhetService orgenhetService;
    @Mock
    RessursStateRepository ressursStateRepository;
    @Mock
    RessursStillingLoadStatus ressursStillingLoadStatus;
    RessursStillingProcessor ressursStillingProcessor;

    @Captor
    ArgumentCaptor<OpprettOrgtilknytning> opprettOrgtilknytningArgumentCaptor;
    @Captor
    ArgumentCaptor<EndreOrgtilknytningDato> korrigerOrgtilknytningDatoArgumentCaptor;
    @Captor
    ArgumentCaptor<AvvikleOrgtilknytning> avvikleOrgtilknytningArgumentCaptor;

    @Captor
    ArgumentCaptor<FjernOrgtilknytning> fjernOrgtilknytningArgumentCaptor;
    @Mock
    NomMastered nomMastered;
    @Mock
    Unleash unleash;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
        ressursStillingProcessor = new RessursStillingProcessor(orgenhetService, null, orgtilknytningCommandService, ressursStillingLoadStatus, ressursStateRepository, nomMastered,
            unleash
        );
    }

    @Test
    @DisplayName("Tester isMatchingPeriod() utility funksjon. Periodene må være like")
    public void test_001() {
        OrgtilknytningState orgtilknytningState = new OrgtilknytningState(null, null, null, false, LocalDate.of(2022, 05, 15), Optional.empty());
        StillingPeriode stillingPeriode = new StillingPeriode(null, LocalDate.of(2022, 05, 15), NO_END_DATE, null);

        // Like fra datoer med åpen slutt dato
        assertTrue(ressursStillingProcessor.isMatchingPeriod(orgtilknytningState, stillingPeriode), "Orgtilknytning: %s\nStillingPeriode: %s".formatted(orgtilknytningState, stillingPeriode));

        // orgtilknytning starter før
        orgtilknytningState = new OrgtilknytningState(null, null, null, false, LocalDate.of(2022, 04, 15), Optional.empty());
        assertFalse(ressursStillingProcessor.isMatchingPeriod(orgtilknytningState, stillingPeriode), "Orgtilknytning: %s\nStillingPeriode: %s".formatted(orgtilknytningState, stillingPeriode));

        // orgtilknytning starter etter
        orgtilknytningState = new OrgtilknytningState(null, null, null, false, LocalDate.of(2022, 06, 15), Optional.empty());
        assertFalse(ressursStillingProcessor.isMatchingPeriod(orgtilknytningState, stillingPeriode), "Orgtilknytning: %s\nStillingPeriode: %s".formatted(orgtilknytningState, stillingPeriode));

        // orgtilknytning slutter før
        orgtilknytningState = new OrgtilknytningState(null, null, null, false, LocalDate.of(2022, 05, 15), Optional.of(LocalDate.of(2024, 06, 15)));
        assertFalse(ressursStillingProcessor.isMatchingPeriod(orgtilknytningState, stillingPeriode), "Orgtilknytning: %s\nStillingPeriode: %s".formatted(orgtilknytningState, stillingPeriode));

        // orgtilknytning slutter på samme sato
        stillingPeriode = new StillingPeriode(null, LocalDate.of(2022, 05, 15), LocalDate.of(2024, 06, 15), null);
        assertTrue(ressursStillingProcessor.isMatchingPeriod(orgtilknytningState, stillingPeriode), "Orgtilknytning: %s\nStillingPeriode: %s".formatted(orgtilknytningState, stillingPeriode));

        // orgtilknytning slutter før, begge med angitt sluttdato
        orgtilknytningState = new OrgtilknytningState(null, null, null, false, LocalDate.of(2022, 05, 15), Optional.of(LocalDate.of(2024, 05, 15)));
        assertFalse(ressursStillingProcessor.isMatchingPeriod(orgtilknytningState, stillingPeriode), "Orgtilknytning: %s\nStillingPeriode: %s".formatted(orgtilknytningState, stillingPeriode));

        // orgtilknytning slutter etter, begge med angitt sluttdato
        orgtilknytningState = new OrgtilknytningState(null, null, null, false, LocalDate.of(2022, 05, 15), Optional.of(LocalDate.of(2024, 07, 15)));
        assertFalse(ressursStillingProcessor.isMatchingPeriod(orgtilknytningState, stillingPeriode), "Orgtilknytning: %s\nStillingPeriode: %s".formatted(orgtilknytningState, stillingPeriode));
    }

    @Test
    @DisplayName("Tester erStillingPeriodenDagligOppfolging() utility funksjon.")
    // Er primær hvis stillingens enhet er match/partial match på ressursens enhet. Og også hvs en av enhetene mangler
    public void test_002() {
        StillingPeriode stillingPeriode = new StillingPeriode(new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("0123456"), null, Optional.of("0123456"), Optional.empty()), LocalDate.of(2022, 05, 15), NO_END_DATE, null);

        // Identisk match på id
        assertTrue(ressursStillingProcessor.erStillingPeriodenDagligOppfolging(stillingPeriode), "StillingPeriode: %s".formatted(stillingPeriode));

        // Partial match på id
        stillingPeriode = new StillingPeriode(new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("01234"), null, Optional.of("0123456"), Optional.empty()), LocalDate.of(2022, 05, 15), NO_END_DATE, null);
        assertTrue(ressursStillingProcessor.erStillingPeriodenDagligOppfolging(stillingPeriode), "StillingPeriode: %s".formatted(stillingPeriode));

        // Partial match på id
        stillingPeriode = new StillingPeriode(new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("0123456"), null, Optional.of("01234"), Optional.empty()), LocalDate.of(2022, 05, 15), NO_END_DATE, null);
        assertTrue(ressursStillingProcessor.erStillingPeriodenDagligOppfolging(stillingPeriode), "StillingPeriode: %s".formatted(stillingPeriode));

        // Stilling mangler enehet
        stillingPeriode = new StillingPeriode(new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.empty(), null, Optional.of("01234"), Optional.empty()), LocalDate.of(2022, 05, 15), NO_END_DATE, null);
        assertTrue(ressursStillingProcessor.erStillingPeriodenDagligOppfolging(stillingPeriode), "StillingPeriode: %s".formatted(stillingPeriode));

        // Ressurs mangler enehet
        stillingPeriode = new StillingPeriode(new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("0123456"), null, Optional.empty(), Optional.empty()), LocalDate.of(2022, 05, 15), NO_END_DATE, null);
        assertTrue(ressursStillingProcessor.erStillingPeriodenDagligOppfolging(stillingPeriode), "StillingPeriode: %s".formatted(stillingPeriode));

        // Ingen match
        stillingPeriode = new StillingPeriode(new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("6543210"), null, Optional.of("0123456"), Optional.empty()), LocalDate.of(2022, 05, 15), NO_END_DATE, null);
        assertFalse(ressursStillingProcessor.erStillingPeriodenDagligOppfolging(stillingPeriode), "StillingPeriode: %s".formatted(stillingPeriode));
    }

    @Test
    @DisplayName("Tester orgtilknytningEnhetMatcherAgressoId() utility funksjon")
    public void test_003() {
        when(orgenhetService.getById("ENHET_1")).thenReturn(orgenhetMedAgressoId("0123456)"));
        when(orgenhetService.getById("ENHET_2")).thenReturn(orgenhetMedAgressoId("6543210)"));

        StillingPeriode stillingPeriode = new StillingPeriode(new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("0123456"), null, Optional.empty(), Optional.empty()), LocalDate.of(2022, 1, 1), NO_END_DATE, null);
        OrgtilknytningState orgtilknytningState = new OrgtilknytningState(null, null, "ENHET_1", false, LocalDate.of(2022, 1, 1), Optional.empty());

        assertTrue(ressursStillingProcessor.orgtilknytningEnhetMatcherAgressoId(stillingPeriode, orgtilknytningState));

        orgtilknytningState = new OrgtilknytningState(null, null, "ENHET_2", false, null, Optional.empty());
        assertFalse(ressursStillingProcessor.orgtilknytningEnhetMatcherAgressoId(stillingPeriode, orgtilknytningState));

        stillingPeriode = new StillingPeriode(new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("6543"), null, Optional.empty(), Optional.empty()), LocalDate.of(2022, 1, 1), NO_END_DATE, null);
        assertTrue(ressursStillingProcessor.orgtilknytningEnhetMatcherAgressoId(stillingPeriode, orgtilknytningState));

        stillingPeriode = new StillingPeriode(new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.empty(), null, Optional.empty(), Optional.empty()), LocalDate.of(2022, 1, 1), NO_END_DATE, null);
        assertFalse(ressursStillingProcessor.orgtilknytningEnhetMatcherAgressoId(stillingPeriode, orgtilknytningState));
    }

    // Disse under er ikke litt waste tester, siden de ikke tester logikk. Men de får stå for nå

    @Test
    @DisplayName("Test opprett ny orgTilknytning")
    public void test_004() {
        StillingPeriode stillingPeriode = new StillingPeriode(
            new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("0123456"), "ENHET_1", Optional.of("0123456"), Optional.of("ORGENHET")),
            LocalDate.now(), null,
            orgenhet("a123bc", "ENHET_1", "0123456")
        );

        ressursStillingProcessor.lagNyOrgtilknytning("A000001", "a123bc", stillingPeriode, null);

        verify(orgtilknytningCommandService, times(1)).opprettOrgtilknytning(opprettOrgtilknytningArgumentCaptor.capture());

        assertEquals("A000001", opprettOrgtilknytningArgumentCaptor.getValue().navident());
        assertEquals("a123bc", opprettOrgtilknytningArgumentCaptor.getValue().orgenhetId());
        assertEquals(LocalDate.now(), opprettOrgtilknytningArgumentCaptor.getValue().gyldigFom());
    }

    @Test
    @DisplayName("Test endre Gyldig FOM på orgTilknytning")
    public void test_005() {
        OrgtilknytningState orginalOrgtilknytningState = new OrgtilknytningState("FID_1", "A000001", "a123bc", false, LocalDate.of(2020, 01, 01), Optional.empty());
        StillingPeriode stillingPeriode = new StillingPeriode(
            new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("0123456"), "ENHET_1", Optional.of("0123456"), Optional.of("ORGENHET")),
            LocalDate.now(), null,
            orgenhet("a123bc", "ENHET_1", "0123456")
        );

        ressursStillingProcessor.endreOrgtilknytningGyldigFOM(orginalOrgtilknytningState, stillingPeriode, null);

        verify(orgtilknytningCommandService, times(1)).endreOrgtilknytningDato(any(), korrigerOrgtilknytningDatoArgumentCaptor.capture());

        assertEquals(orginalOrgtilknytningState.navident(), korrigerOrgtilknytningDatoArgumentCaptor.getValue().navident());
        assertEquals(orginalOrgtilknytningState.orgEnhetId(), korrigerOrgtilknytningDatoArgumentCaptor.getValue().orgenhetId());
        assertEquals(LocalDate.now(), korrigerOrgtilknytningDatoArgumentCaptor.getValue().gyldigFom());
        assertEquals(orginalOrgtilknytningState.gyldigTom(), korrigerOrgtilknytningDatoArgumentCaptor.getValue().gyldigTom());
    }


    @Test
    @DisplayName("Test sette Gyldig TOM på orgTilknytning via endreOrgtilknytningGyldigTOM")
    void test_006() {
        OrgtilknytningState orginalOrgtilknytningState = new OrgtilknytningState("FID_1", "A000001", "a123bc", false, LocalDate.of(2020, 01, 01), Optional.empty());

        StillingPeriode stillingPeriode = new StillingPeriode(
            new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("0123456"), "ENHET_1", Optional.of("0123456"), Optional.of("ORGENHET")),
            LocalDate.of(2020, 01, 01), LocalDate.now(),
            orgenhet("a123bc", "ENHET_1", "0123456")
        );

        ressursStillingProcessor.endreOrgtilknytningGyldigTOM(orginalOrgtilknytningState, stillingPeriode, null);

        verify(orgtilknytningCommandService, times(1)).avvikleOrgtilknytning(any(), avvikleOrgtilknytningArgumentCaptor.capture());

        assertEquals(orginalOrgtilknytningState.navident(), avvikleOrgtilknytningArgumentCaptor.getValue().navident());
        assertEquals(orginalOrgtilknytningState.orgEnhetId(), avvikleOrgtilknytningArgumentCaptor.getValue().orgenhetId());
        assertEquals(LocalDate.now(), avvikleOrgtilknytningArgumentCaptor.getValue().gyldigTom());
    }

    @Test
    @DisplayName("Test endre eksisterende Gyldig TOM på orgTilknytning")
    void test_007() {
        OrgtilknytningState orginalOrgtilknytningState = new OrgtilknytningState("FID_1", "A000001", "a123bc", false, LocalDate.of(2020, 01, 01), Optional.of(LocalDate.of(2022, 12, 31)));

        StillingPeriode stillingPeriode = new StillingPeriode(
            new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("0123456"), "ENHET_1", Optional.of("0123456"), Optional.of("ORGENHET")),
            LocalDate.of(2020, 01, 01), LocalDate.now(),
            orgenhet("a123bc", "ENHET_1", "0123456")
        );

        ressursStillingProcessor.endreOrgtilknytningGyldigTOM(orginalOrgtilknytningState, stillingPeriode, null);

        verify(orgtilknytningCommandService, times(1)).endreOrgtilknytningDato(any(), korrigerOrgtilknytningDatoArgumentCaptor.capture());

        assertEquals(orginalOrgtilknytningState.navident(), korrigerOrgtilknytningDatoArgumentCaptor.getValue().navident());
        assertEquals(orginalOrgtilknytningState.orgEnhetId(), korrigerOrgtilknytningDatoArgumentCaptor.getValue().orgenhetId());
        assertEquals(orginalOrgtilknytningState.gyldigFom(), korrigerOrgtilknytningDatoArgumentCaptor.getValue().gyldigFom());
        assertEquals(LocalDate.now(), korrigerOrgtilknytningDatoArgumentCaptor.getValue().gyldigTom().get());
    }

    @Test
    @DisplayName("Test sette Gyldig TOM på orgTilknytning via settOrgtilknytningSluttDato")
    void test_008() {
        OrgtilknytningState orginalOrgtilknytningState = new OrgtilknytningState("FID_1", "A000001", "a123bc", false, LocalDate.of(2020, 01, 01), Optional.empty());


        StillingPeriode stillingPeriode = new StillingPeriode(
            new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("0123456"), "ENHET_1", Optional.of("0123456"), Optional.of("ORGENHET")),
            LocalDate.of(2020, 01, 01), LocalDate.now(),
            orgenhet("a123bc", "ENHET_1", "0123456")
        );

        ressursStillingProcessor.settOrgtilknytningSluttDato(orginalOrgtilknytningState.fid(), orginalOrgtilknytningState.navident(), stillingPeriode, null);

        verify(orgtilknytningCommandService, times(1)).avvikleOrgtilknytning(any(), avvikleOrgtilknytningArgumentCaptor.capture());

        assertEquals(orginalOrgtilknytningState.navident(), avvikleOrgtilknytningArgumentCaptor.getValue().navident());
        assertEquals(orginalOrgtilknytningState.orgEnhetId(), avvikleOrgtilknytningArgumentCaptor.getValue().orgenhetId());
        assertEquals(LocalDate.now(), avvikleOrgtilknytningArgumentCaptor.getValue().gyldigTom());
    }

    @Test
    @DisplayName("Test fjerne orgTilknytning")
    void test_009() {
        OrgtilknytningState orginalOrgtilknytningState = new OrgtilknytningState("FID_1", "A000001", "a123bc", false, LocalDate.of(2020, 01, 01), Optional.empty());

        ressursStillingProcessor.fjernOrgtilknytning(orginalOrgtilknytningState);

        verify(orgtilknytningCommandService, times(1)).fjernOrgtilknytning(eq(orginalOrgtilknytningState.fid()), fjernOrgtilknytningArgumentCaptor.capture());

        assertEquals(orginalOrgtilknytningState.orgEnhetId(), fjernOrgtilknytningArgumentCaptor.getValue().orgenhetId());
        assertEquals(LocalDate.of(2020, 01, 01), fjernOrgtilknytningArgumentCaptor.getValue().gyldigFom());
    }

    private IOrgenhet orgenhet(String id, String navn, String agressoId) {
        return new OrgEnhet(new OrgEnhetState(null, id, navn, null, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(agressoId), Optional.empty(), Optional.empty()));
    }


    private Optional<IOrgenhet> orgenhetMedAgressoId(String agressoId) {
        return Optional.of(new OrgEnhet(new OrgEnhetState(null, null, null, null, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(agressoId), Optional.empty(), Optional.empty())));
    }
}