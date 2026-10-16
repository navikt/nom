package no.nav.nom.apps.sfintegrasjon.organisasjon.eventsource;

import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhetleder.NomStateOrgEnhetLederListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EvtsrcStartupTest {
    private final NomStateRessursListener ressurs = mock(NomStateRessursListener.class);
    private final NomStateOrgEnhetListener orgEnhet = mock(NomStateOrgEnhetListener.class);
    private final NomStateOrganiseringListener organisering = mock(NomStateOrganiseringListener.class);
    private final NomStateOrgtilknytningListener orgtilknytning = mock(NomStateOrgtilknytningListener.class);
    private final NomStateMasteredRessursListener masteredRessurs = mock(NomStateMasteredRessursListener.class);
    private final NomStateDelegertLederListener delegertLeder = mock(NomStateDelegertLederListener.class);
    private final NomStateOrgEnhetLederListener orgEnhetLeder = mock(NomStateOrgEnhetLederListener.class);
    private final KafkaListenerEndpointRegistry registry = mock(KafkaListenerEndpointRegistry.class);
    private final ApplicationArguments args = mock(ApplicationArguments.class);

    private EvtsrcStartup startup(boolean autoStartup, Duration timeout) {
        return new EvtsrcStartup(ressurs, orgEnhet, organisering, orgtilknytning,
            masteredRessurs, delegertLeder, orgEnhetLeder, registry, autoStartup, timeout);
    }

    private void allStatesReady() {
        when(ressurs.hasReadToEndOnStartup()).thenReturn(true);
        when(orgEnhet.hasReadToEndOnStartup()).thenReturn(true);
        when(organisering.hasReadToEndOnStartup()).thenReturn(true);
        when(orgtilknytning.hasReadToEndOnStartup()).thenReturn(true);
        when(masteredRessurs.hasReadToEndOnStartup()).thenReturn(true);
        when(delegertLeder.hasReadToEndOnStartup()).thenReturn(true);
        when(orgEnhetLeder.hasReadToEndOnStartup()).thenReturn(true);
    }

    @Test
    void startsAllEvtsrcListenersOnlyAfterAllStatesAreReady() throws Exception {
        allStatesReady();
        var orgEnhetContainer = mock(MessageListenerContainer.class);
        var lederContainer = mock(MessageListenerContainer.class);
        var organiseringContainer = mock(MessageListenerContainer.class);
        when(registry.getListenerContainer(EvtsrcKafkaListener.ORGENHET_LISTENER_ID)).thenReturn(orgEnhetContainer);
        when(registry.getListenerContainer(EvtsrcKafkaListener.ORGENHET_LEDER_LISTENER_ID)).thenReturn(lederContainer);
        when(registry.getListenerContainer(EvtsrcKafkaListener.ORGANISERING_LISTENER_ID)).thenReturn(organiseringContainer);

        startup(true, Duration.ofSeconds(1)).run(args);

        verify(orgEnhetContainer).start();
        verify(lederContainer).start();
        verify(organiseringContainer).start();
    }

    @Test
    void waitsForTheLastStateListenerBeforeStarting() throws Exception {
        allStatesReady();
        when(orgEnhetLeder.hasReadToEndOnStartup()).thenReturn(false, true);
        var container = mock(MessageListenerContainer.class);
        when(registry.getListenerContainer(EvtsrcKafkaListener.ORGENHET_LISTENER_ID)).thenReturn(container);
        when(registry.getListenerContainer(EvtsrcKafkaListener.ORGENHET_LEDER_LISTENER_ID)).thenReturn(container);
        when(registry.getListenerContainer(EvtsrcKafkaListener.ORGANISERING_LISTENER_ID)).thenReturn(container);

        startup(true, Duration.ofSeconds(2)).run(args);

        verify(orgEnhetLeder, atLeast(2)).hasReadToEndOnStartup();
        verify(container, atLeast(1)).start();
    }

    @Test
    void doesNotStartWhenStateIsNotReadyBeforeTimeout() {
        assertThatThrownBy(() -> startup(true, Duration.ZERO).run(args))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ikke ferdig innlest");

        verifyNoInteractions(registry);
    }

    @Test
    void doesNotStartWhenStateConsumptionIsAborted() {
        when(orgEnhetLeder.isAbortedOnStartup()).thenReturn(true);

        assertThatThrownBy(() -> startup(true, Duration.ofSeconds(1)).run(args))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("avbrutt");

        verifyNoInteractions(registry);
    }

    @Test
    void respectsDisabledEvtsrcStartup() throws Exception {
        startup(false, Duration.ofSeconds(1)).run(args);

        verifyNoInteractions(registry);
        verify(orgEnhetLeder, never()).hasReadToEndOnStartup();
    }
}
