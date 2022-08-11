package no.nav.nom.apps.navident.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@TestConfiguration
@Slf4j
public class TestRedisConfiguration {

    private RedisServer redisServer;

    public TestRedisConfiguration(@Value("${spring.redis.port}") final int redisPort) {
        //this.redisServer = new RedisServer(redisPort);
        this.redisServer = RedisServer.builder().setting("maxmemory 16MB").port(redisPort).setting("bind localhost").build();
        log.debug("Redis port(s): ".formatted(this.redisServer.ports()));
    }

    @PostConstruct
    public void postConstruct() {
        redisServer.start();
    }

    @PreDestroy
    public void preDestroy() {
        redisServer.stop();
    }
}
