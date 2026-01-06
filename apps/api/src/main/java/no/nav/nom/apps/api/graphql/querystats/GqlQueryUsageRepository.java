package no.nav.nom.apps.api.graphql.querystats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GqlQueryUsageRepository extends JpaRepository<GqlQueryUsage, Integer> {
    List<GqlQueryUsage> findByHash(String hash);
    List<GqlQueryUsage> findByConsumer(String consumer);

    @Modifying
    @Transactional
    @Query("DELETE FROM GqlQueryUsage WHERE timestamp BETWEEN :from AND :to")
    int deleteUsageBetweenDates(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT timestamp FROM GqlQueryUsage WHERE timestamp < :beforeDate")
    List<LocalDateTime> findTimestamps(@Param("beforeDate") LocalDateTime beforeDate);
}
