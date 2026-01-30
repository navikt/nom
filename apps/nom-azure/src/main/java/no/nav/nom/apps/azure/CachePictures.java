package no.nav.nom.apps.azure;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Service
public class CachePictures {

    private final Logger log = LoggerFactory.getLogger(CachePictures.class);

    private final AzureAdService azureAdService;
    private final CacheValkey cacheValkey;

    public CachePictures(AzureAdService azureAdService, MeterRegistry meterRegistry, RedisTemplate<String, String> redisTemplate) {
        this.azureAdService = azureAdService;
        this.cacheValkey = new CacheValkey(redisTemplate, meterRegistry);
    }

    public Optional<byte[]> get(String navident) {
        return this.cacheValkey.get(navident);
    }

    public void refetch(String navident) {
        this.cacheValkey.refresh(navident);
    }

    /**
     * Caching av bilder basert på oppkobling mot valkey instans
     */
    private class CacheValkey {

        private static final String CODE_NO_PICTURE = "NO_PICTURE_404";
        private static final Duration DEFAULT_TTL = Duration.ofHours(72);

        private final RedisTemplate<String, String> redisTemplate;
        private final Gauge cacheValkeySizeGauge;
        private final Counter countValkeyPictureGet;


        public CacheValkey(RedisTemplate<String, String> redisTemplate, MeterRegistry meterRegistry) {
            this.redisTemplate = redisTemplate;

            this.cacheValkeySizeGauge = Gauge
                .builder("picture.cache.valkey.size", this::valkeyCacheSize)
                .register(meterRegistry);

            this.countValkeyPictureGet = meterRegistry.counter("requests.picture_requested.valkey.count");

        }

        private Long valkeyCacheSize() {
            try {
                log.debug("checking valkey cache size");
                var connectionFactory = this.redisTemplate.getConnectionFactory();
                if (connectionFactory != null) {
                    var conn = connectionFactory.getConnection();
                    var dbSize = conn.serverCommands().dbSize();
                    conn.close();
                    log.debug("got valkey cache size: " + dbSize);
                    return dbSize;
                }
            } catch (Throwable ignored) {
            }
            log.debug("Failed to get valkey cache size");
            return -1L;

        }

        public Optional<byte[]> get(String navident) {
            String maybeBase64Img = this.redisTemplate.opsForValue().get(navidentToKeyPath(navident));
            if (maybeBase64Img == null) {
                log.debug("Missing from cache, need to fetch for navident: " + navident);
                return fetchAndUpdate(navident);
            }

            if (CODE_NO_PICTURE.equals(maybeBase64Img)) {
                log.debug("Entry for missing picture found in cache for navident: " + navident);
                return Optional.empty();
            }
            var bytesAsBase64 = Base64.getDecoder().decode(maybeBase64Img);
            this.countValkeyPictureGet.increment();
            return Optional.of(bytesAsBase64);
        }

        public void refresh(String navident) {
            fetchAndUpdate(navident);
        }

        private Optional<byte[]> fetchAndUpdate(String navident) {
            var maybeFetched = azureAdService.fetchProfilePictureByNavIdent(navident);
            if (maybeFetched.isPresent()) {
                String bytesAsBase64 = Base64.getEncoder().encodeToString(maybeFetched.get());
                log.debug("Storing picture in redis cache for navident: " + navident);
                this.redisTemplate.opsForValue().set(navidentToKeyPath(navident), bytesAsBase64, DEFAULT_TTL);
            } else {
                log.debug("Storing 'not found' entry in redis cache for navident: " + navident);
                this.redisTemplate.opsForValue().set(navidentToKeyPath(navident), CODE_NO_PICTURE, DEFAULT_TTL);
            }
            cacheValkeySizeGauge.value();
            return maybeFetched;
        }

        private String navidentToKeyPath(String navident) {
            return "nom.azure.base64_bildedata." + navident;
        }
    }

}
