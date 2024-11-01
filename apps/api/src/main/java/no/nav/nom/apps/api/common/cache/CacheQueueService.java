package no.nav.nom.apps.api.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.api.email.OTEndretData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class CacheQueueService {
    private final CacheQueueRepository cacheQueueRepository;

    public CacheQueueService(CacheQueueRepository cacheQueueRepository) {
        this.cacheQueueRepository = cacheQueueRepository;
    }

    public void save(String navident, OTEndretData otEndretData) throws IOException {
        CacheQueue existingCacheQueue = findQueue(navident, CacheType.OTENDRINGER.toString());

        if(existingCacheQueue != null) {
            log.info("CacheQueue already exists for {}. Cache will be updated. Existing: {}, to be added: {}", navident, existingCacheQueue, otEndretData);
            ObjectReader reader = new ObjectMapper().readerFor(new TypeReference<List<OTEndretData>>() {});
            List<OTEndretData> payload = reader.readValue(existingCacheQueue.getPayload());
            payload.add(otEndretData);

            existingCacheQueue.setPayload(new ObjectMapper().convertValue(removeDuplicateOTChangesEntries(payload), JsonNode.class));
            log.info("CacheQueue updated: {}", existingCacheQueue);
            cacheQueueRepository.save(existingCacheQueue);
        } else {
            log.info("CacheQueue does not exists for {}. New element will be added: {}", navident, otEndretData);
            ArrayList<OTEndretData> payload = new ArrayList<>() {{add(otEndretData);}};
            CacheQueue cacheQueue = new CacheQueue();
            cacheQueue.setCacheKey(navident);
            cacheQueue.setCacheType(CacheType.OTENDRINGER.toString());
            cacheQueue.setPayload(new ObjectMapper().convertValue(payload, JsonNode.class));
            log.info("CacheQueue added: {}", cacheQueue);
            cacheQueueRepository.save(cacheQueue);
        }
    }

    public List<OTEndretData> removeDuplicateOTChangesEntries(List<OTEndretData> otEndringerData) {
        return new ArrayList<>(new HashSet<>(otEndringerData));
    }

    public void delete(String cacheKey, String cacheType) {
        CacheQueue cacheQueue = findQueue(cacheKey, cacheType);
        if(cacheQueue == null) {
            log.warn("CacheQueue not found for cacheKey: {} and cacheType: {}", cacheKey, cacheType);
            return;
        }
        cacheQueueRepository.delete(cacheQueue);
    }

    public CacheQueue findQueue(String cacheKey, String cacheType) {
        return cacheQueueRepository.findQueue(cacheKey, cacheType).orElse(null);
    }

    public Map<String, List<OTEndretData>> getOTEndringerFromCache() {
        Map <String, List<OTEndretData>> otEndringer = new HashMap<>();
        List<CacheQueue> cacheQueues = cacheQueueRepository.findAllByType(CacheType.OTENDRINGER.toString());
        ObjectReader reader = new ObjectMapper().readerFor(new TypeReference<List<OTEndretData>>() {});
        cacheQueues.forEach(cacheQueue -> {
            try {
                List<OTEndretData> payload = reader.readValue(cacheQueue.getPayload());
                otEndringer.put(cacheQueue.getCacheKey(), payload);
            } catch (Exception e) {
                log.error("Error while parsing payload from cacheQueue", e);
            }
        });

        return otEndringer;
    }

}
