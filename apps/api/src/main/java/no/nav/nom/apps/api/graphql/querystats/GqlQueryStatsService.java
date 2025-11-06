package no.nav.nom.apps.api.graphql.querystats;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class GqlQueryStatsService {
    private final GqlQueryRepository gqlQueryRepository;
    private final GqlQueryUsageRepository gqlQueryUsageRepository;
    private final GqlFieldUsageRepository gqlFieldUsageRepository;

    public GqlQueryStatsService(GqlQueryRepository gqlQueryRepository, GqlQueryUsageRepository gqlQueryUsageRepository,
                                GqlFieldUsageRepository gqlFieldUsageRepository
    ) {
        this.gqlQueryRepository = gqlQueryRepository;
        this.gqlQueryUsageRepository = gqlQueryUsageRepository;
        this.gqlFieldUsageRepository = gqlFieldUsageRepository;
    }

    public void registerQueryUsage(String query, String consumer) {
        String hash = getHashAndInsertIfMissing(query);
        gqlQueryUsageRepository.save(
            GqlQueryUsage.builder()
                .hash(hash)
                .consumer(consumer)
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    public void registerFieldUsage(String field, String path, String operationName, String consumer) {
        gqlFieldUsageRepository.save(
            GqlFieldUsage.builder()
                .field(field)
                .path(path)
                .operationName(operationName)
                .consumer(consumer)
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    private String getHashAndInsertIfMissing(String query) {
        String hash = hash(query);
        gqlQueryRepository.findByHash(hash).ifPresentOrElse(
            gqlQuery -> {},
            () -> gqlQueryRepository.save(GqlQuery.builder()
                .hash(hash)
                .query(query)
                .build())
        );
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

