package no.nav.nom.apps.api.orgenhet;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import static no.nav.nom.apps.api.common.SpringCacheConfig.AKTIVE_ORGENHETER_I_HIERARKI;

@Slf4j
@Component
public class AktiveOrgenheterIHierarkiCache {
    private final OrganiseringStateRepository organiseringStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final AppProperties.Orgenhet orgenhetProperties;

    public AktiveOrgenheterIHierarkiCache(
        OrganiseringStateRepository organiseringStateRepository,
        OrgEnhetStateRepository orgEnhetStateRepository,
        AppProperties appProperties
    ) {
        this.organiseringStateRepository = organiseringStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgenhetProperties = appProperties.orgenhet();
    }

    /**
     * Henter alle aktive enheter som har en aktiv enhet i hierarkiet under NAV
     * Ekskluderer HELFO, SKATT, RIKSREV og VIKAFOSSEN
     *
     * Caches i 5 min
     */
    @Cacheable(cacheNames = AKTIVE_ORGENHETER_I_HIERARKI, cacheManager = "cacheManager")
    public Collection<IOrgenhet> getAktiveEnheterIOrganisasjonen() {
        var aktiveOrgenhetIder = organiseringStateRepository.getEnhetOgOrganiseringerNedover(orgenhetProperties.navId()).stream()
            .filter(organiseringStateRepository::isActive)
            .map(OrganiseringState::id)
            .collect(Collectors.toSet());

        return orgEnhetStateRepository.findByIdIn(aktiveOrgenhetIder)
            .stream()
            .map(OrgEnhet::new)
            .collect(Collectors.toSet());
    }

    @CacheEvict(value = AKTIVE_ORGENHETER_I_HIERARKI, allEntries = true)
    public void emptyCache() {
        log.info("Emptying AKTIVE_ORGENHETER_I_HIERARKI cache");
    }
}

// Invalider og re-bygg state cache så fort det er endring i orgenhet eller organisering
@Slf4j
@Component
class DebounceRebuildCacheService {
    private static final long DEBOUNCE_SECONDS = 5;

    private final TaskScheduler taskScheduler;
    private final AktiveOrgenheterIHierarkiCache cache;

    private ScheduledFuture<?> scheduledTask;

    public DebounceRebuildCacheService(AktiveOrgenheterIHierarkiCache cache,
                                       NomStateOrgEnhetListener nomStateOrgEnhetListener,
                                       NomStateOrganiseringListener nomStateOrganiseringListener
    ) {
        this.taskScheduler = new ConcurrentTaskScheduler(Executors.newSingleThreadScheduledExecutor());
        this.cache = cache;

        listenToStateChanges(nomStateOrgEnhetListener, nomStateOrganiseringListener);
    }

    // Schedule rebuild in 5sec. If any other task already exists, (within 5sec) remove old one
    // E.g. wait for 5 seconds of silence before rebuilding cache
    // Do not mind if rebuild cach task triggering happens exactly when this new request is received
    public void requestRebuildCache() {
        synchronized (this) {
            if (scheduledTask != null && !scheduledTask.isDone()) {
                scheduledTask.cancel(false);
            }
            scheduledTask = taskScheduler.schedule(this::rebuildCacheTask, Instant.now().plusSeconds(DEBOUNCE_SECONDS));
        }
    }

    private void rebuildCacheTask() {
        synchronized (this) {
            scheduledTask = null;
        }
        cache.emptyCache();
        cache.getAktiveEnheterIOrganisasjonen();
        log.info("Done rebuilding AktiveOrgenheterIHierarkiCache");
    }

    private void listenToStateChanges(NomStateOrgEnhetListener nomStateOrgEnhetListener, NomStateOrganiseringListener nomStateOrganiseringListener) {
        nomStateOrgEnhetListener.register(new AbstractNomStateListener.Visitor<>() {
            @Override
            public String visitorName() {
                return "AktiveOrgenheterIHierarkiCache_ListenToOrgenhetChanges";
            }
            @Override
            public void visit(String key, OrgEnhetState stateObject) {
                requestRebuildCache();
            }
        });

        nomStateOrganiseringListener.register(new AbstractNomStateListener.Visitor<>() {
            @Override
            public String visitorName() {
                return "AktiveOrgenheterIHierarkiCache_ListenToOrganiseringChanges";
            }
            @Override
            public void visit(String key, OrganiseringState stateObject) {
                requestRebuildCache();
            }
        });
    }
}