package no.nav.nom.apps.api.orgtilknytning.eventsourcing.state;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class ShadowOrgtilknytningerKommunaleRepository {
    private final Map<String, OrgtilknytningState> orgtilknytningByNavident = new ConcurrentHashMap<>();
    private AtomicBoolean initialized = new AtomicBoolean(false);

    public void resetWithNewData(Map<String, OrgtilknytningState> newOrgtilknytningByNavident) {
        orgtilknytningByNavident.clear();
        orgtilknytningByNavident.putAll(newOrgtilknytningByNavident);
        log.info("Lastet {} kommunale skygge orgtilknytninger", orgtilknytningByNavident.size());
        initialized.set(true);
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public OrgtilknytningState getOrgtilknytningByNavident(String navIdent) {
        return orgtilknytningByNavident.get(navIdent);
    }

    public Collection<OrgtilknytningState> getAll() {
        return orgtilknytningByNavident.values();
    }
}
