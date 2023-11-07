package no.nav.nom.apps.navident.common.cache;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.common.LogConfig;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Use to synchronize multiple invocations to betstillIdent() for the same personident.
 * To prevent multiple creations of NavIdent for the same person in case of a client side 'double-click'
 */
@Component
@Slf4j
public class RedisSynchronizationCache {

    private final RedisLockRegistry redisPersonidentLockRegistry;

    public RedisSynchronizationCache(RedisLockRegistry redisPersonidentLockRegistry) {
        this.redisPersonidentLockRegistry = redisPersonidentLockRegistry;
    }

    public boolean tryLock(String personid) {
        cleanupOldLocks();

        log.debug(LogConfig.SECURE_LOGS, "Locking personid %s".formatted(personid));
        log.debug("Locking personid %s".formatted(mask(personid)));
        return redisPersonidentLockRegistry.obtain(personid).tryLock();
    }

    public void unlock(String personid) {
        log.debug(LogConfig.SECURE_LOGS, "Unlocking personident %s".formatted(personid));
        log.debug("Unlocking personident %s".formatted(mask(personid)));
        redisPersonidentLockRegistry.obtain(personid).unlock();
    }

    private void cleanupOldLocks() {
        // If locks has not been unlocked within 20 seconds, they are considered old and will be removed
        redisPersonidentLockRegistry.expireUnusedOlderThan(20000);
    }

    private String mask(String toBeMasked) {
        if (toBeMasked == null || toBeMasked.length() < 7) return toBeMasked;
        return toBeMasked.substring(0, 6) + "***";
    }
}
