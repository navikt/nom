package no.nav.nom.apps.api.graphql.querystats;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class GqlStatConsolidator {

    final GqlConsolidatedFieldUsageRepository gqlConsolidatedFieldUsageRepository;
    final GqlConsolidatedQueryUsageRepository gqlConsolidatedQueryUsageRepository;
    final GqlFieldUsageRepository gqlFieldUsageRepository;
    final GqlQueryUsageRepository gqlQueryUsageRepository;
    final LeaderElection leaderElection;


    public GqlStatConsolidator(GqlConsolidatedFieldUsageRepository gqlConsolidatedFieldUsageRepository,
                               GqlConsolidatedQueryUsageRepository gqlConsolidatedQueryUsageRepository, GqlFieldUsageRepository gqlFieldUsageRepository,
                               GqlQueryUsageRepository gqlQueryUsageRepository, LeaderElection leaderElection
    ) {
        this.gqlConsolidatedFieldUsageRepository = gqlConsolidatedFieldUsageRepository;
        this.gqlConsolidatedQueryUsageRepository = gqlConsolidatedQueryUsageRepository;
        this.gqlFieldUsageRepository = gqlFieldUsageRepository;
        this.gqlQueryUsageRepository = gqlQueryUsageRepository;
        this.leaderElection = leaderElection;
    }

    @Scheduled(cron = "0 10 1 * * *")
    @Transactional
    public void consolidateAllMinus30Days() {
        if (!leaderElection.isLeader()) {
            return;
        }
        var minus30days = LocalDate.now().minusDays(30);

        var fieldTimestamps = gqlFieldUsageRepository.findTimestamps(minus30days.atStartOfDay());
        var queryTimestamps = gqlQueryUsageRepository.findTimestamps(minus30days.atStartOfDay());

        Set<LocalDate> dates = new HashSet<>();
        dates.addAll(fieldTimestamps.stream().map(LocalDateTime::toLocalDate).distinct().toList());
        dates.addAll(queryTimestamps.stream().map(LocalDateTime::toLocalDate).distinct().toList());

        dates.stream()
            .sorted()
            .forEach(date -> {
                var result = consolidateGQLUsage(date);
                log.info("Daily consolidation for {} completed: \n{}", date, result);
            });
    }


    @Transactional
    public String consolidateGQLUsage(LocalDate date) {
        log.info("Consolidating GQL field usage for date {}", date);

        var from = date.atStartOfDay();
        var to = date.plusDays(1).atStartOfDay();

        int insertedRows = gqlConsolidatedFieldUsageRepository.insertConsolidatedUsageBetweenDates(
            from,
            to,
            date.getYear(),
            date.getMonthValue(),
            date.getDayOfMonth()
        );

        int deletedRows = gqlFieldUsageRepository.deleteUsageBetweenDates(from, to);
        var fieldResult = String.format("Inserted %d consolidated rows, deleted %d raw rows", insertedRows, deletedRows);

        insertedRows = gqlConsolidatedQueryUsageRepository.insertConsolidatedUsageBetweenDates(
            from,
            to,
            date.getYear(),
            date.getMonthValue(),
            date.getDayOfMonth()
        );

        deletedRows = gqlQueryUsageRepository.deleteUsageBetweenDates(from, to);
        var queryResult = String.format("Inserted %d consolidated rows, deleted %d raw rows", insertedRows, deletedRows);

        return String.format("Field usage:\n\t%s\nQuery usage:\n\t%s", fieldResult, queryResult);
    }
}
