package no.nav.nom.apps.sfintegrasjon.organisasjon.eventsource;

import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhetleder.NomStateOrgEnhetLederListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class EvtsrcStartup implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(EvtsrcStartup.class);

    private final List<AbstractNomStateListener<?>> stateListeners;
    private final KafkaListenerEndpointRegistry registry;
    private final boolean autoStartup;
    private final Duration timeout;

    public EvtsrcStartup(
        NomStateRessursListener ressurs,
        NomStateOrgEnhetListener orgEnhet,
        NomStateOrganiseringListener organisering,
        NomStateOrgtilknytningListener orgtilknytning,
        NomStateMasteredRessursListener masteredRessurs,
        NomStateDelegertLederListener delegertLeder,
        NomStateOrgEnhetLederListener orgEnhetLeder,
        KafkaListenerEndpointRegistry registry,
        @Value("${app.kafka.evtsrc.auto-startup:true}") boolean autoStartup,
        @Value("${app.kafka.evtsrc.state-ready-timeout:PT10M}") Duration timeout
    ) {
        this.stateListeners = List.of(ressurs, orgEnhet, organisering, orgtilknytning,
            masteredRessurs, delegertLeder, orgEnhetLeder);
        this.registry = registry;
        this.autoStartup = autoStartup;
        this.timeout = timeout;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!autoStartup) {
            return;
        }

        long deadline = System.nanoTime() + timeout.toNanos();
        while (!stateListeners.stream().allMatch(AbstractNomStateListener::hasReadToEndOnStartup)) {
            if (stateListeners.stream().anyMatch(AbstractNomStateListener::isAbortedOnStartup)) {
                throw new IllegalStateException("Innlesing av NOM state ble avbrutt. Starter ikke evtsrc-listenere.");
            }
            if (System.nanoTime() >= deadline) {
                throw new IllegalStateException("NOM state ble ikke ferdig innlest innen " + timeout + ". Starter ikke evtsrc-listenere.");
            }
            TimeUnit.MILLISECONDS.sleep(250);
        }

        // Feil under oppstart av en container skal stoppe oppstarten, ikke la poden bli klar med bare noen listenere.
        start(EvtsrcKafkaListener.ORGENHET_LISTENER_ID);
        start(EvtsrcKafkaListener.ORGENHET_LEDER_LISTENER_ID);
        start(EvtsrcKafkaListener.ORGANISERING_LISTENER_ID);
    }

    private void start(String listenerId) {
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container == null) {
            throw new IllegalStateException("Fant ikke evtsrc-listener: " + listenerId);
        }
        log.info("Starter evtsrc-listener [{}] etter at NOM state er lest til enden", listenerId);
        container.start();
    }
}
