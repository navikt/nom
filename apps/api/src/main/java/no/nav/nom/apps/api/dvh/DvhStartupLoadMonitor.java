package no.nav.nom.apps.api.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.gql.lucenesearch.ReadyForIndexingInterface;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class DvhStartupLoadMonitor implements ReadyForIndexingInterface {

    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);

    public boolean hasCompleted() {
        return initialLoadComplete.get();
    }

    public void setCompleted() {
        initialLoadComplete.set(true);
    }

    public void awaitInitialLoadComplete() {
        while ( !hasCompleted() ) { // Wait on initial DVH load to complete
            try {
                Thread.sleep(1000L);
            } catch (Exception ignored) {
                log.error("Waiting for initial DVH last feilet", ignored);
            }
        }
    }

    @Override
    public boolean isReadyForIndexing() {
        return hasCompleted();
    }
}
