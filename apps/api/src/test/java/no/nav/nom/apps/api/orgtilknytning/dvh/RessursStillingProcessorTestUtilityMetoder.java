package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.AvvikleOrgEnhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.KorrigerOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
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

import static no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingProcessor.NO_END_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@DisplayName("Tester Utility metoder for bruk i tolking og generering av events for RessursStillinger fra DVH")
class RessursStillingProcessorTestUtilityMetoder {


    @Mock
    OrgtilknytningCommandService orgtilknytningCommandService;

    @Mock
    OrgenhetService orgenhetService;

    RessursStillingProcessor ressursStillingProcessor;

    @Captor
    ArgumentCaptor<OpprettOrgtilknytning> opprettOrgtilknytningArgumentCaptor;
    @Captor
    ArgumentCaptor<KorrigerOrgtilknytningDato> korrigerOrgtilknytningDatoArgumentCaptor;
    @Captor
    ArgumentCaptor<AvvikleOrgtilknytning> avvikleOrgtilknytningArgumentCaptor;


    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
        ressursStillingProcessor = new RessursStillingProcessor(orgenhetService, null, orgtilknytningCommandService);
    }

    @Test
    @DisplayName("Tester isMatchingPeriod() utility funksjon. Periodene må være like")
    public void test_isMatchingPeriod() {
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
    public void test_erStillingPeriodenDagligOppfolging() {
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
    public void test_orgtilknytningEnhetMatcherAgressoId() {
        when(orgenhetService.getById("ENHET_1")).thenReturn(orgenhetMedAgressoId("0123456)"));
        when(orgenhetService.getById("ENHET_2")).thenReturn(orgenhetMedAgressoId("6543210)"));

        StillingPeriode stillingPeriode = new StillingPeriode(new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("0123456"), null, Optional.empty(), Optional.empty()), null, null, null);
        OrgtilknytningState orgtilknytningState = new OrgtilknytningState(null, null, "ENHET_1", false, null, Optional.empty());

        assertTrue(ressursStillingProcessor.orgtilknytningEnhetMatcherAgressoId(stillingPeriode, orgtilknytningState));

        orgtilknytningState = new OrgtilknytningState(null, null, "ENHET_2", false, null, Optional.empty());
        assertFalse(ressursStillingProcessor.orgtilknytningEnhetMatcherAgressoId(stillingPeriode, orgtilknytningState));

        stillingPeriode = new StillingPeriode(new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("6543"), null, Optional.empty(), Optional.empty()), null, null, null);
        assertTrue(ressursStillingProcessor.orgtilknytningEnhetMatcherAgressoId(stillingPeriode, orgtilknytningState));

        stillingPeriode = new StillingPeriode(new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.empty(), null, Optional.empty(), Optional.empty()), null, null, null);
        assertFalse(ressursStillingProcessor.orgtilknytningEnhetMatcherAgressoId(stillingPeriode, orgtilknytningState));
    }

    @Test
    @DisplayName("Test opprett ny orgTilknytning")
    public void test_lagNyOrgtilknytning() {
        StillingPeriode stillingPeriode = new StillingPeriode(
            new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("0123456"), "ENHET_1", Optional.of("0123456"), Optional.of("ORGENHET")),
            LocalDate.now(), null,
            orgenhet("a123bc", "ENHET_1", "0123456")
        );

        ressursStillingProcessor.lagNyOrgtilknytning("A000001", "a123bc", stillingPeriode, null);

        verify(orgtilknytningCommandService, times(1)).opprettOrgtilknytning(opprettOrgtilknytningArgumentCaptor.capture());

        assertEquals("A000001", opprettOrgtilknytningArgumentCaptor.getValue().navident());
        assertEquals("a123bc", opprettOrgtilknytningArgumentCaptor.getValue().orgEnhetId());
        assertEquals(LocalDate.now(), opprettOrgtilknytningArgumentCaptor.getValue().gyldigFom());
    }

    @Test
    @DisplayName("Test endre Gyldig FOM på orgTilknytning")
    public void test_endreOrgtilknytningGyldigFOM() {
        OrgtilknytningState orginalOrgtilknytningState = new OrgtilknytningState("FID_1", "A000001", "a123bc", false, LocalDate.of(2020, 01, 01), Optional.empty());
        StillingPeriode stillingPeriode = new StillingPeriode(
            new StillingPeriode.StillingAgressoOrgIdOgNavn(Optional.of("0123456"), "ENHET_1", Optional.of("0123456"), Optional.of("ORGENHET")),
            LocalDate.now(), null,
            orgenhet("a123bc", "ENHET_1", "0123456")
        );

        ressursStillingProcessor.endreOrgtilknytningGyldigFOM(orginalOrgtilknytningState, stillingPeriode, null);

        verify(orgtilknytningCommandService, times(1)).korrigerOrgtilknytningDato(any(), korrigerOrgtilknytningDatoArgumentCaptor.capture());

        assertEquals(orginalOrgtilknytningState.navident(), korrigerOrgtilknytningDatoArgumentCaptor.getValue().navident());
        assertEquals(orginalOrgtilknytningState.orgEnhetId(), korrigerOrgtilknytningDatoArgumentCaptor.getValue().orgEnhetId());
        assertEquals(LocalDate.now(), korrigerOrgtilknytningDatoArgumentCaptor.getValue().gyldigFom());
        assertEquals(orginalOrgtilknytningState.gyldigTom(), korrigerOrgtilknytningDatoArgumentCaptor.getValue().gyldigTom());
    }

    private IOrgenhet orgenhet(String id, String navn, String agressoId) {
        return new OrgEnhet(new OrgEnhetState(null, id, navn, null, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(agressoId), Optional.empty(), Optional.empty()));
    }

    private Optional<IOrgenhet> orgenhetMedAgressoId(String agressoId) {
        return Optional.of(new OrgEnhet(new OrgEnhetState(null, null, null, null, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(agressoId), Optional.empty(), Optional.empty())));
    }
}