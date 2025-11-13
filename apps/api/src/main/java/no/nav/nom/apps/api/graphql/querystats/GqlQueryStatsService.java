package no.nav.nom.apps.api.graphql.querystats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.common.metrics.FieldPathEntry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class GqlQueryStatsService {
    private static final int QUEUE_MONITOR_WARNING_SIZE = 50;
    private final Set<String> knownHashes = new HashSet<>(); // simple caching
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GqlQueryRepository gqlQueryRepository;
    private final GqlQueryUsageRepository gqlQueryUsageRepository;
    private final GqlFieldUsageRepository gqlFieldUsageRepository;
    private final ThreadPoolTaskExecutor taskExecutor;

    public GqlQueryStatsService(
        GqlQueryRepository gqlQueryRepository,
       GqlQueryUsageRepository gqlQueryUsageRepository,
        GqlFieldUsageRepository gqlFieldUsageRepository,
        @Qualifier("gqlStatsTaskExecutor") ThreadPoolTaskExecutor taskExecutor
    ) {
        this.gqlQueryRepository = gqlQueryRepository;
        this.gqlQueryUsageRepository = gqlQueryUsageRepository;
        this.gqlFieldUsageRepository = gqlFieldUsageRepository;
        this.taskExecutor = taskExecutor;
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
            JsonNode json = objectMapper.valueToTree(fieldPathList);
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
        monitorQueueSize();
        taskExecutor.execute(task);
    }

    private void monitorQueueSize() {
        log.debug("GraphQL TaskExecutor queue current size: {}", taskExecutor.getQueueSize());
        if (taskExecutor.getQueueSize() > QUEUE_MONITOR_WARNING_SIZE) {
            log.error("GraphQL TaskExecutor queue size has passed {} items in queue. Current size {}", QUEUE_MONITOR_WARNING_SIZE, taskExecutor.getQueueSize());
        }
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
        }
        knownHashes.add(hash);
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

