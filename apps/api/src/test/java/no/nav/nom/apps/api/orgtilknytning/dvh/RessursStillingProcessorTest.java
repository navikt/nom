package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Tester Utility metoder for bruk i tolking og generering av events for RessursStillinger fra DVH")

class RessursStillingProcessorTest {

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
}