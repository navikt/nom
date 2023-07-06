package no.nav.nom.apps.navident.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@Slf4j
public class TestRedisConfiguration {

    static {
        GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);
        redis.start();
        System.setProperty("spring.data.redis.host", redis.getHost());
        System.setProperty("spring.data.redis.port", redis.getMappedPort(6379).toString());
    }
}
