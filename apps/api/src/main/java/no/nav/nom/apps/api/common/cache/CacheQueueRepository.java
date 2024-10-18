package no.nav.nom.apps.api.common.cache;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CacheQueueRepository extends JpaRepository<CacheQueue, CacheID> {
    @Query("SELECT q FROM CacheQueue q WHERE q.cacheKey = :cacheKey AND q.cacheType = :cacheType")
    Optional<CacheQueue> findQueue(String cacheKey, String cacheType);

    @Query("SELECT q FROM CacheQueue q WHERE q.cacheType = :cacheType")
    List<CacheQueue> findAllByType(String cacheType);
}
