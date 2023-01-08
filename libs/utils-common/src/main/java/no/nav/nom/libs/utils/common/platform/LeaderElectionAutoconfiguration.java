package no.nav.nom.libs.utils.common.platform;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;

@AutoConfiguration
@AutoConfigureAfter(RestTemplateAutoConfiguration.class)
@Conditional(LeaderElectionAutoconfiguration.LeaderElectionEnabledCondition.class)
public class LeaderElectionAutoconfiguration {

    @Bean
    @ConditionalOnMissingBean
    LeaderElection leaderElection(RestTemplateBuilder restTemplateBuilder, Environment environment) {
        return new LeaderElection(restTemplateBuilder, environment);
    }

    static class LeaderElectionEnabledCondition extends AllNestedConditions {
        LeaderElectionEnabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "nom.libs.utils.common.leader-election.enabled", havingValue = "true", matchIfMissing = true)
        static class isEnabled {}

        @ConditionalOnProperty(LeaderElection.ELECTOR_PATH_NAME)
        static class HasElectorPath {}

        @ConditionalOnBean(RestTemplateBuilder.class)
        static class HasRestTemplateBuilder {}
    }
}
