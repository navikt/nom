package no.nav.nom.apps.navident.common.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

@Configuration
public class RedisLockRegistryConfig {
    @Bean
    RedisLockRegistry redisPersonidentLockRegistry(JedisConnectionFactory jedisConnectionFactory) {
        RedisLockRegistry lockRegistry = new RedisLockRegistry(jedisConnectionFactory, "redis-lock-on-personident-registry");
        // https://docs.spring.io/spring-integration/reference/redis.html#redis-lock-registry
        lockRegistry.setRedisLockType(RedisLockRegistry.RedisLockType.PUB_SUB_LOCK);
        return lockRegistry;
    }
}
