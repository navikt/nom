package no.nav.nom.apps.api.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class CacheQueueRepositoryTest {

    @Autowired
    CacheQueueRepository cacheQueueRepository;

    private CacheQueue cacheQueue;

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        cacheQueue = new CacheQueue();
        cacheQueue.setCacheType(CacheType.OTENDRINGER.toString());
        cacheQueue.setCacheKey("cacheKey");
        cacheQueue.setPayload(new ObjectMapper().readTree("{\"key\": \"value\"}"));
        cacheQueueRepository.save(cacheQueue);
    }

    @After
    public void tearDown() {
        cacheQueueRepository.delete(cacheQueue);
    }

    @Test
    public void testFindQueue() {
        // setup
        String cacheKey = "cacheKey";
        String cacheType = CacheType.OTENDRINGER.name();

        // execute
        CacheQueue result = cacheQueueRepository.findQueue(cacheKey, cacheType).orElse(null);

        // verify
        Assertions.assertEquals(cacheQueue, result);
    }

    @Test
    public void testFindQueueWithInvalidCacheKey() {
        // setup
        String cacheKey = "invalidCacheKey";
        String cacheType = CacheType.OTENDRINGER.name();

        // execute
        CacheQueue result = cacheQueueRepository.findQueue(cacheKey, cacheType).orElse(null);

        // verify
        Assertions.assertNull(result);
    }

    @Test
    public void testDelete() {
        // setup
        String cacheKey = "cacheKey";
        String cacheType = CacheType.OTENDRINGER.name();

        // execute
        CacheQueue existingCacheQueue = cacheQueueRepository.findQueue(cacheKey, cacheType).orElse(null);
        assert existingCacheQueue != null;
        cacheQueueRepository.delete(existingCacheQueue);

        // verify
        CacheQueue result = cacheQueueRepository.findQueue(cacheKey, cacheType).orElse(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testDuplicateKeysOverwrites() throws Exception {
        // setup
        CacheQueue cacheQueueDuplicateKey = new CacheQueue();
        cacheQueueDuplicateKey.setCacheType(CacheType.OTENDRINGER.toString());
        cacheQueueDuplicateKey.setCacheKey("cacheKey");
        cacheQueueDuplicateKey.setPayload(new ObjectMapper().readTree("{\"key2\": \"value2\"}"));
        cacheQueueRepository.save(cacheQueueDuplicateKey);

        // execute
        CacheQueue resultDuplicateKey = cacheQueueRepository.findQueue(cacheQueueDuplicateKey.getCacheKey(), cacheQueueDuplicateKey.getCacheType()).orElse(null);

        // verify
        assert resultDuplicateKey != null;
        Assertions.assertEquals(resultDuplicateKey.getCacheKey(), cacheQueue.getCacheKey());
        Assertions.assertEquals(resultDuplicateKey.getCacheType(), cacheQueue.getCacheType());
        Assertions.assertNotEquals(resultDuplicateKey.getPayload(), cacheQueue.getPayload());
    }

    @Test
    public void testUniqueKeyAddNewEntry() throws Exception {
        // setup
        CacheQueue cacheQueueDuplicateKey = new CacheQueue();
        cacheQueueDuplicateKey.setCacheType(CacheType.OTENDRINGER.toString());
        cacheQueueDuplicateKey.setCacheKey("cacheKey");
        cacheQueueDuplicateKey.setPayload(new ObjectMapper().readTree("{\"key2\": \"value2\"}"));
        cacheQueueRepository.save(cacheQueueDuplicateKey);

        CacheQueue cacheQueueNewKey = new CacheQueue();
        cacheQueueNewKey.setCacheType(CacheType.OTENDRINGER.toString());
        cacheQueueNewKey.setCacheKey("cacheKey3");
        cacheQueueNewKey.setPayload(new ObjectMapper().readTree("{\"key3\": \"value3\"}"));
        cacheQueueRepository.save(cacheQueueNewKey);

        // execute
        CacheQueue resultDuplicateKey = cacheQueueRepository.findQueue(cacheQueueDuplicateKey.getCacheKey(), cacheQueueDuplicateKey.getCacheType()).orElse(null);
        CacheQueue resultNewKey = cacheQueueRepository.findQueue(cacheQueueNewKey.getCacheKey(), cacheQueueNewKey.getCacheType()).orElse(null);

        // verify
        assert resultDuplicateKey != null;
        assert resultNewKey != null;
        Assertions.assertEquals(resultDuplicateKey.getCacheKey(), cacheQueue.getCacheKey());
        Assertions.assertEquals(resultDuplicateKey.getCacheType(), cacheQueue.getCacheType());
        Assertions.assertNotEquals(resultDuplicateKey.getPayload(), cacheQueue.getPayload());

        Assertions.assertNotEquals(resultNewKey.getCacheKey(), cacheQueue.getCacheKey());
        Assertions.assertEquals(resultNewKey.getCacheType(), cacheQueue.getCacheType());
        Assertions.assertNotEquals(resultNewKey.getPayload(), cacheQueue.getPayload());
    }

}
