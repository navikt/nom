package no.nav.nom.apps.api.graphql.querystats;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.metrics.FieldPathEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GqlQueryStatsService {
    private final Set<String> knownHashes = ConcurrentHashMap.newKeySet(); // simple caching
    private final JsonMapper jsonMapper = new JsonMapper();

    private final GqlQueryRepository gqlQueryRepository;
    private final GqlQueryUsageRepository gqlQueryUsageRepository;
    private final GqlFieldUsageRepository gqlFieldUsageRepository;
    private final ThreadPoolTaskExecutor taskExecutor;

    public GqlQueryStatsService(
        GqlQueryRepository gqlQueryRepository,
        GqlQueryUsageRepository gqlQueryUsageRepository,
        GqlFieldUsageRepository gqlFieldUsageRepository,
        @Autowired(required = false) MeterRegistry meterRegistry,
        @Qualifier("gqlStatsTaskExecutor") ThreadPoolTaskExecutor taskExecutor
    ) {
        this.gqlQueryRepository = gqlQueryRepository;
        this.gqlQueryUsageRepository = gqlQueryUsageRepository;
        this.gqlFieldUsageRepository = gqlFieldUsageRepository;
        this.taskExecutor = taskExecutor;

        if(meterRegistry != null){
            log.info("Registering gauge for " + GqlQueryStatsService.class.getName() + " field usage queue size");
            Gauge.builder("nom_querystatsservice_queue_size", taskExecutor, it -> it.getQueueSize() + 0.0).register(meterRegistry);
        }
    }

    public void registerQueryUsage(String query, String consumer) {
        execute(() -> {
            String hash = getHashAndInsertIfMissing(query);
            gqlQueryUsageRepository.save(
                GqlQueryUsage.builder()
                    .hash(hash)
                    .consumer(consumer)
                    .timestamp(LocalDateTime.now())
                    .build()
            );
        });
    }

    public void registerFieldUsage(List<FieldPathEntry> fieldPathList, String operationName, String consumer) {
        execute(() -> {
            JsonNode json = jsonMapper.valueToTree(fieldPathList);
            String jsonArray = json.toString();

            log.debug("Field/path jsonArray, operation and consumer: {}, {} and {}", jsonArray, operationName, consumer);
            gqlFieldUsageRepository.save(
                GqlFieldUsage.builder()
                    .fieldPathArray(json)
                    .operationName(operationName)
                    .consumer(consumer)
                    .timestamp(LocalDateTime.now())
                    .build()
            );
        });
    }

    private void execute(Runnable task) {
        taskExecutor.execute(task);
    }

    private String getHashAndInsertIfMissing(String query) {
        String hash = hash(query);
        if (!knownHashes.contains(hash)) {
            gqlQueryRepository.findByHash(hash).ifPresentOrElse(
                _ -> {},
                () -> gqlQueryRepository.save(GqlQuery.builder()
                    .hash(hash)
                    .query(query)
                    .build())
            );
            knownHashes.add(hash);
        }
        return hash;
    }

    private static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashedBytes = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 Hashing algorithm not found", e);
        }
    }
}

