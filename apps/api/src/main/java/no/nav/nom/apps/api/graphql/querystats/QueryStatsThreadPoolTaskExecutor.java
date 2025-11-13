package no.nav.nom.apps.api.graphql.querystats;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class QueryStatsThreadPoolTaskExecutor {

    @Bean
    ThreadPoolTaskExecutor gqlStatsTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(1);
        taskExecutor.setMaxPoolSize(4);
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setThreadNamePrefix("GQL_Metric_Saver-");
        return taskExecutor;
    }
}
