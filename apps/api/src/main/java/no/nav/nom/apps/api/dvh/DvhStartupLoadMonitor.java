package no.nav.nom.apps.api.dvh;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class DvhStartupLoadMonitor {

    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);

    public boolean hasCompleted() {
        return initialLoadComplete.get();
    }

    public void setCompleted() {
        initialLoadComplete.set(true);
    }
}
