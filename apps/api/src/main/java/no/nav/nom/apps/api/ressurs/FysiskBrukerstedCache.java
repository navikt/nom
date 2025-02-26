package no.nav.nom.apps.api.ressurs;

import lombok.extern.slf4j.Slf4j;
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
    @Autowired
    private NomRemedyIntegrasjonClient nomRemedyIntegrasjonClient;

    @Cacheable(FYSISK_BRUKERSTED_CACHE_NAME)
    public List<FysiskBrukersted> getAll() {
        return nomRemedyIntegrasjonClient.getFysiskeBrukersteder();
    }

    @CacheEvict(value = FYSISK_BRUKERSTED_CACHE_NAME, allEntries = true)
    @Scheduled(fixedRateString = CACHE_TTL)
    public void emptyCache() {
        log.info("Emptying Fysiske brukersteder cache");
    }
}
