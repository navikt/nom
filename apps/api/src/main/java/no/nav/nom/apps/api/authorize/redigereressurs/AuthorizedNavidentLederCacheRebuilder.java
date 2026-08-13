package no.nav.nom.apps.api.authorize.redigereressurs;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhetleder.NomStateOrgEnhetLederListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

/**
 * Bygger "redigere ressurs"-auth-cachen i {@link AuthorizedNavident} på nytt når relevant state endres.
 *
 * <p>Cachen ({@code lederForOrgenhetSomKanRedigere}) avhenger av tre state-kilder:
 * orgenhetleder (hvem som er leder), organisering (hierarkiet) og orgenhet (aktive enheter/nivå).
 * Vi registrerer derfor en visitor på hver av de tre state-listenerne og trigger
 * {@link AuthorizedNavident#oppdaterLedereForOrgenheter()} når noen av dem endres.
 *
 * <p>Rebuild er debouncet: hver endring avbryter og planlegger oppgaven {@value #DEBOUNCE_SECONDS} sekunder frem,
 * slik at en byge av endringer (f.eks. under DVH-lasten) kollapser til én rebuild.
 * Fordi rebuild kjøres på en egen scheduler-tråd (ikke visitor-tråden) vil en feil her ikke velte
 * Kafka-listeneren.
 *
 * <p>Erstatter tidligere synkrone kall til {@code oppdaterLedereForOrgenheter()} fra
 * {@code OrgEnhetLederService}/{@code OrgEnhetRestService} (som skapte en sirkulær avhengighet) og de
 * eksplisitte kallene i {@code DvhRestConsumer}. Følger samme mønster som {@code DebounceRebuildCacheService}.
 */
@Slf4j
@Component
class AuthorizedNavidentLederCacheRebuilder {
    private static final long DEBOUNCE_SECONDS = 5;

    private final TaskScheduler taskScheduler = new ConcurrentTaskScheduler(Executors.newSingleThreadScheduledExecutor());
    private final AuthorizedNavident authorizedNavident;

    private ScheduledFuture<?> scheduledTask;

    AuthorizedNavidentLederCacheRebuilder(
        AuthorizedNavident authorizedNavident,
        NomStateOrgEnhetLederListener orgEnhetLederListener,
        NomStateOrgEnhetListener orgEnhetListener,
        NomStateOrganiseringListener organiseringListener
    ) {
        this.authorizedNavident = authorizedNavident;
        registrerVisitor(orgEnhetLederListener, "OrgEnhetLeder");
        registrerVisitor(orgEnhetListener, "OrgEnhet");
        registrerVisitor(organiseringListener, "Organisering");
    }

    private <T> void registrerVisitor(AbstractNomStateListener<T> listener, String kilde) {
        listener.register(new AbstractNomStateListener.Visitor<T>() {
            @Override
            public String visitorName() {
                return "AuthedLedereCacheRebuilder_" + kilde;
            }

            @Override
            public void visit(String key, T stateObject) {
                requestRebuild();
            }
        });
    }

    void requestRebuild() {
        synchronized (this) {
            if (scheduledTask != null && !scheduledTask.isDone()) {
                scheduledTask.cancel(false);
            }
            scheduledTask = taskScheduler.schedule(this::rebuild, Instant.now().plusSeconds(DEBOUNCE_SECONDS));
        }
    }

    private void rebuild() {
        synchronized (this) {
            scheduledTask = null;
        }
        try {
            authorizedNavident.oppdaterLedereForOrgenheter();
        } catch (Exception e) {
            log.error("Feilet i debounced rebuild av redigere-ressurs auth-cache", e);
        }
    }
}
