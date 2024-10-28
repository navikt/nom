package no.nav.nom.apps.api.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import no.nav.nom.apps.api.orgtilknytning.api.email.OTEndretData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ActiveProfiles("test")
@ExtendWith({SpringExtension.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class CacheQueueServiceTest {

    @Autowired
    CacheQueueRepository cacheQueueRepository;

    private CacheQueueService cacheQueueService;

    @BeforeEach
    public void setUp() {
        cacheQueueRepository.deleteAll();
        cacheQueueService = new CacheQueueService(cacheQueueRepository);
    }
    @Test
    public void testSaveAndFindQueueMethods() throws IOException {
        // setup
        String fid = "fid123456";
        String navident = "A123456";
        String navidentChangedBy = "C123456";
        String timestamp = LocalDateTime.now().toString();
        OTEndretData otEndringerData =
            OTEndretData.builder()
                .fid(fid)
                .navident(navident)
                .changedByNavIdent(navidentChangedBy)
                .timestamp(timestamp)
                .eventType(OTEndretData.OTEndretDataType.OT_NY)
                .build();

        // execute
        cacheQueueService.save(navident, otEndringerData);

        // assert
        CacheQueue queue = cacheQueueService.findQueue(navident, CacheType.OTENDRINGER.toString());
        ObjectReader reader = new ObjectMapper().readerFor(new TypeReference<List<OTEndretData>>() {});
        List<OTEndretData> otEndringer = reader.readValue(queue.getPayload());
        assertEquals(1, otEndringer.size());
        assertEquals(otEndringerData, otEndringer.get(0));

        // add another entry and verify
        OTEndretData otEndringerData2 =
            OTEndretData.builder()
                .fid(fid)
                .changedByNavIdent(navidentChangedBy)
                .timestamp(timestamp)
                .eventType(OTEndretData.OTEndretDataType.OT_NY)
                .build();
        cacheQueueService.save(navident, otEndringerData2);
        queue = cacheQueueService.findQueue(navident, CacheType.OTENDRINGER.toString());
        otEndringer = reader.readValue(queue.getPayload());
        assertEquals(2, otEndringer.size());
    }

    @Test
    public void testDelete() throws IOException {
        // setup
        String fid = "fid123456";
        String navident = "A123456";
        String navidentChangedBy = "C123456";
        String timestamp = LocalDateTime.now().toString();
        OTEndretData otEndringerData =
            OTEndretData.builder()
                .fid(fid)
                .navident(navident)
                .changedByNavIdent(navidentChangedBy)
                .timestamp(timestamp)
                .eventType(OTEndretData.OTEndretDataType.OT_NY)
                .build();
        cacheQueueService.save(navident, otEndringerData);

        // execute
        cacheQueueService.delete(navident, CacheType.OTENDRINGER.toString());
        CacheQueue queue = cacheQueueService.findQueue(navident, CacheType.OTENDRINGER.toString());

        // assert
        assertNull(queue);
    }

    @Test
    public void testgetOTEndringerFromCache() throws IOException {
        // setup
        String fid = "fid123456";
        String navident1 = "A123456";
        String navident2 = "B123456";
        String navidentChangedBy = "C123456";
        String timestamp = LocalDateTime.now().toString();
        OTEndretData otEndringerData =
            OTEndretData.builder()
                .fid(fid)
                .navident(navident1)
                .changedByNavIdent(navidentChangedBy)
                .timestamp(timestamp)
                .eventType(OTEndretData.OTEndretDataType.OT_NY)
                .build();

        // execute
        cacheQueueService.save(navident1, otEndringerData);
        Map<String, List<OTEndretData>> endringer = cacheQueueService.getOTEndringerFromCache();

        // assert
        assertEquals(1, endringer.size());

        // add another entry and verify
        cacheQueueService.save(navident2, otEndringerData);
        endringer = cacheQueueService.getOTEndringerFromCache();
        assertEquals(2, endringer.size());
    }
}
