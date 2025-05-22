package no.nav.nom.apps.azure;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.getunleash.Unleash;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Base64;


import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;

@Service
public class CachePictures {

    private final Logger log = LoggerFactory.getLogger(CachePictures.class);

    private final AzureAdService azureAdService;
    private final ICachePicture cacheCaffeine;
    private final ICachePicture cacheValkey;
    private final Unleash unleash;

    public CachePictures(AzureAdService azureAdService, MeterRegistry meterRegistry, RedisTemplate<String, String> redisTemplate, Unleash unleash) {
        this.azureAdService = azureAdService;
        this.cacheCaffeine = new CacheCaffeine(meterRegistry);
        this.unleash = unleash;
        this.cacheValkey = new CacheValkey(redisTemplate, meterRegistry);
    }

    public Optional<byte[]> get(String navident) {
        switch(getCacheProvider()){
            case CAFFEINE -> {
                return this.cacheCaffeine.get(navident);
            }
            case VALKEY -> {
                return this.cacheValkey.get(navident);
            }
        }
        return Optional.empty();
    }

    public void refetch(String navident) {
        switch(getCacheProvider()){
            case CAFFEINE -> {
                this.cacheCaffeine.refresh(navident);
            }
            case VALKEY -> {
                this.cacheValkey.refresh(navident);
            }
        }
    }


    private CacheProvider getCacheProvider() {
        var shouldUseRedis = this.unleash.isEnabled("nom.azure.bilder.brukredis");
        if(shouldUseRedis) {
            return CacheProvider.VALKEY;
        }
        return CacheProvider.CAFFEINE;
    }


    private enum CacheProvider {
        CAFFEINE,
        VALKEY;
    }

    private interface ICachePicture {
        Optional<byte[]> get(String navIdent);
        void refresh(String navident);
    }

    /**
     * In memory caching av bilder basert på Caffeine
     */
    private class CacheCaffeine implements ICachePicture {
        private final AsyncLoadingCache<String, Optional<byte[]>> pictureCache;
        private final Gauge cacheCaffeineSizeGauge;
        private final Counter countCaffeinePictureGet;


        public CacheCaffeine(MeterRegistry meterRegistry) {

            this.pictureCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(72))
                .refreshAfterWrite(Duration.ofHours(24))
                .maximumSize(20000)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .buildAsync(azureAdService::fetchProfilePictureByNavIdent);

            this.cacheCaffeineSizeGauge = Gauge
                .builder("picture.cache.caffeine.size", this.pictureCache, (asyncLoadingCache) -> asyncLoadingCache.synchronous().estimatedSize())
                .register(meterRegistry);

            this.countCaffeinePictureGet =  meterRegistry.counter("requests.picture_requested.caffeine.count");
        }
        @Override
        public Optional<byte[]> get(String navIdent) {
            this.cacheCaffeineSizeGauge.value();
            try{
                this.countCaffeinePictureGet.increment();
                return pictureCache.get(navIdent).join();
            } catch (RuntimeException e) {
                log.error(e.getMessage());
                return Optional.empty();
            }
        }
        @Override
        public void refresh(String navident){
            this.pictureCache.synchronous().refresh(navident);
        }
    }

    /**
     * Caching av bilder basert på oppkobling mot valkey instans
     */
    private class CacheValkey implements ICachePicture {

        private static final String CODE_NO_PICTURE = "NO_PICTURE_404";

        private final RedisTemplate<String, String> redisTemplate;
        private final Gauge cacheValkeySizeGauge;
        private final Counter countValkeyPictureGet;


        public CacheValkey(RedisTemplate<String, String> redisTemplate, MeterRegistry meterRegistry) {
            this.redisTemplate = redisTemplate;

            this.cacheValkeySizeGauge = Gauge
                .builder("picture.cache.valkey.size", this::valkeyCacheSize)
                .register(meterRegistry);

            this.countValkeyPictureGet =  meterRegistry.counter("requests.picture_requested.valkey.count");

        }

        private Long valkeyCacheSize(){
            try {
                var connectionFactory = this.redisTemplate.getConnectionFactory();
                if (connectionFactory != null) {
                    return connectionFactory.getConnection().serverCommands().dbSize();
                }
            } catch (Throwable e) {
                return -1L;
            }
            return -1L;
        }

        @Override
        public Optional<byte[]> get(String navident) {
            String maybeBase64Img = this.redisTemplate.opsForValue().get(navidentToKeyPath(navident));
            if (maybeBase64Img == null) {
                return fetchAndUpdate(navident);
            }
            if (CODE_NO_PICTURE.equals(maybeBase64Img)) {
                return Optional.empty();
            }
            var bytesAsBase64 = Base64.getDecoder().decode(maybeBase64Img);
            this.countValkeyPictureGet.increment();
            return Optional.of(bytesAsBase64);
        }

        @Override
        public void refresh(String navident) {
            fetchAndUpdate(navident);
        }

        private Optional<byte[]> fetchAndUpdate(String navident){
            var maybeFetched = azureAdService.fetchProfilePictureByNavIdent(navident);
            if (maybeFetched.isPresent()) {
                String bytesAsBase64 = Base64.getEncoder().encodeToString(maybeFetched.get());
                this.redisTemplate.opsForValue().set(navidentToKeyPath(navident), bytesAsBase64, Duration.ofHours(72));
            } else {
                this.redisTemplate.opsForValue().set(navidentToKeyPath(navident), CODE_NO_PICTURE, Duration.ofHours(72));
            }
            cacheValkeySizeGauge.value();
            return maybeFetched;
        }

        private String navidentToKeyPath(String navident){
            return "nom.azure.base64_bildedata." + navident;
        }
    }

}
