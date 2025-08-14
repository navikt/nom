package no.nav.nom.apps.api.common;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class SpringCacheConfig {
    public static final String FYSISK_BRUKERSTED_CACHE_NAME = "fysiskeBrukersteder";
    public static final String STATISTIKK_REST_SERVICE_CACHE_NAME = "statistikkRestServiceCache";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(FYSISK_BRUKERSTED_CACHE_NAME, STATISTIKK_REST_SERVICE_CACHE_NAME);
    }

}
