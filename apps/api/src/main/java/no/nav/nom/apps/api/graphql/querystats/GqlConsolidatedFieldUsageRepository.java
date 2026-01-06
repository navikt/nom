package no.nav.nom.apps.api.graphql.querystats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface GqlConsolidatedFieldUsageRepository extends JpaRepository<GqlConsolidatedFieldUsage, Integer> {

    @Modifying
    @Transactional
    @Query(
        value =
            "INSERT INTO t_gql_consolidated_field_usage (field_path_array, consumer, year, month, day, count) " +
            "SELECT field_path_array, consumer, :year, :month, :day, count(*) " +
            "FROM t_gql_field_usage " +
            "WHERE timestamp BETWEEN :from AND :to\n" +
            "GROUP BY field_path_array, consumer",
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
