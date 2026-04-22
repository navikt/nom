package no.nav.nom.apps.api.ressurs;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.ressurs.api.app.MainManagerClient;
import no.nav.nom.apps.api.ressurs.api.app.NomRemedyIntegrasjonClient;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static no.nav.nom.apps.api.common.SpringCacheConfig.FYSISK_BRUKERSTED_CACHE_NAME;
import static no.nav.nom.apps.api.ressurs.FysiskBrukerstedService.CACHE_TTL;

@Slf4j
@Component
public class FysiskBrukerstedCache {

    private final Unleash unleash;
    public static final String UNLEASH_USEMAINMANAGER_FOR_FYSISK_BRUKERSTED = "nom.brukersted.useMainManagerForFysiskBrukersted";

    public FysiskBrukerstedCache(Unleash unleash) {
        this.unleash = unleash;
    }

    @Autowired
    private NomRemedyIntegrasjonClient nomRemedyIntegrasjonClient;

    @Autowired
    private MainManagerClient mainManagerClient;

    @Cacheable(cacheNames = FYSISK_BRUKERSTED_CACHE_NAME, cacheManager = "cacheManager")
    public List<FysiskBrukersted> getAll() {
        var brukMainManager = unleash.isEnabled(UNLEASH_USEMAINMANAGER_FOR_FYSISK_BRUKERSTED);
        return brukMainManager?mainManagerClient.getFysiskeBrukersteder() : nomRemedyIntegrasjonClient.getFysiskeBrukersteder();
    }

    @CacheEvict(value = FYSISK_BRUKERSTED_CACHE_NAME, allEntries = true)
    @Scheduled(fixedRateString = CACHE_TTL)
    public void emptyCache() {
        log.info("Emptying Fysiske brukersteder cache");
    }
}
