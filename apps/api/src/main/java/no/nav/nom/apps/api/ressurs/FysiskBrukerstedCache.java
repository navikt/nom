package no.nav.nom.apps.api.ressurs;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.ressurs.api.app.MainManagerClient;
import no.nav.nom.apps.api.ressurs.api.app.NomRemedyIntegrasjonClient;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.List;

import static no.nav.nom.apps.api.common.SpringCacheConfig.FYSISK_BRUKERSTED_CACHE_NAME;
import static no.nav.nom.apps.api.ressurs.FysiskBrukerstedService.CACHE_TTL;

@Slf4j
@Component
public class FysiskBrukerstedCache {

    static final int MAX_RETRY_ATTEMPTS = 3;
    static final long RETRY_DELAY_MS = 100L;

    private final Unleash unleash;
    private final CacheManager cacheManager;
    public static final String UNLEASH_USEMAINMANAGER_FOR_FYSISK_BRUKERSTED = "nom.brukersted.useMainManagerForFysiskBrukersted";

    public FysiskBrukerstedCache(Unleash unleash, CacheManager cacheManager) {
        this.unleash = unleash;
        this.cacheManager = cacheManager;
    }

    @Autowired
    private NomRemedyIntegrasjonClient nomRemedyIntegrasjonClient;

    @Autowired
    private MainManagerClient mainManagerClient;

    @Cacheable(cacheNames = FYSISK_BRUKERSTED_CACHE_NAME, cacheManager = "cacheManager")
    public List<FysiskBrukersted> getAll() {
        return fetchWithRetry();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        log.info("Varmer opp Fysiske brukersteder cache ved oppstart");
        refreshCache();
    }

    @Scheduled(fixedRateString = CACHE_TTL, initialDelayString = CACHE_TTL)
    public void refreshCache() {
        log.info("Oppdaterer Fysiske brukersteder cache");
        try {
            var freshData = fetchWithRetry();
            var cache = cacheManager.getCache(FYSISK_BRUKERSTED_CACHE_NAME);
            if (cache != null) {
                cache.put(SimpleKey.EMPTY, freshData);
                log.info("Fysiske brukersteder cache oppdatert med {} oppføringer", freshData.size());
            }
        } catch (Exception e) {
            log.error("Feil ved refresh av Fysiske brukersteder cache, beholder eksisterende data", e);
        }
    }

    @CacheEvict(value = FYSISK_BRUKERSTED_CACHE_NAME, allEntries = true)
    public void emptyCache() {
        log.info("Tømmer Fysiske brukersteder cache");
    }

    private List<FysiskBrukersted> fetchWithRetry() {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return fetchFromSource();
            } catch (WebClientRequestException e) {
                lastException = e;
                log.warn("Forsøk {} av {} feilet ved henting av fysiske brukersteder: {}", attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Avbrutt ved retry av henting av fysiske brukersteder", ie);
                    }
                }
            }
        }
        log.error("ALERT: Feil ved henting av fysiske brukersteder (fra {})",  unleash.isEnabled(UNLEASH_USEMAINMANAGER_FOR_FYSISK_BRUKERSTED) ? "MainManager" : "NomRemedyIntegrasjon", lastException);
        throw new RuntimeException("Alle " + MAX_RETRY_ATTEMPTS + " forsøk på å hente fysiske brukersteder feilet", lastException);
    }

    private List<FysiskBrukersted> fetchFromSource() {
        var brukMainManager = unleash.isEnabled(UNLEASH_USEMAINMANAGER_FOR_FYSISK_BRUKERSTED);
        return brukMainManager ? mainManagerClient.getFysiskeBrukersteder() : nomRemedyIntegrasjonClient.getFysiskeBrukersteder();
    }
}
