package no.nav.nom.apps.navident.common.redis;

import no.nav.nom.apps.navident.SpringBootTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(TestRedisConfiguration.class)
class BasicEmbeddedRedisServerTest extends SpringBootTestBase {
    @Autowired RedisCacheManager cacheManager;

    @Test
    public void testCache() {
        Cache cache = cacheManager.getCache("TestCache");
        assertNotNull(cache);
        cache.put("test", "test");
    }

}