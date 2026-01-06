package no.nav.nom.apps.api.graphql.querystats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface GqlConsolidatedQueryUsageRepository extends JpaRepository<GqlConsolidatedQueryUsage, Integer> {

    @Modifying
    @Transactional
    @Query(
        value =
            "INSERT INTO t_gql_consolidated_query_usage (hash, consumer, year, month, day, count) " +
            "SELECT hash, consumer, :year, :month, :day, count(*) " +
            "FROM t_gql_query_usage " +
            "WHERE timestamp BETWEEN :from AND :to\n" +
            "GROUP BY hash, consumer",
        nativeQuery = true
    )
    int insertConsolidatedUsageBetweenDates(
        @Param("from") LocalDateTime from,
        @Param("to")LocalDateTime to,
        @Param("year")Integer year,
        @Param("month")Integer month,
        @Param("day")Integer day
    );
}
