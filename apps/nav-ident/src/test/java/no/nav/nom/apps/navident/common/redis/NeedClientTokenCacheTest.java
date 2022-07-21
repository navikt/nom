package no.nav.nom.apps.navident.common.redis;

import no.nav.nom.apps.navident.SpringBootTestBase;
import no.nav.nom.apps.navident.common.cache.NeedClientTokenCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(TestRedisConfiguration.class)
@EnableCaching
class NeedClientTokenCacheTest extends SpringBootTestBase {
    @Autowired NeedClientTokenCache cache;

    @Test
    public void cachePutAndHit() {
        String uuid = "UUID_1";
        String token = "TOKEN_1";
        cache.putToken(uuid, token);
        String hit = cache.getToken(uuid);

        assertEquals(token, hit);
    }
}
