package no.nav.nom.apps.api.graphql.search;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.graphql.GraphQLTestUtil;
import no.nav.nom.apps.api.graphql.common.config.GraphQLConfig;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.gql.GraphQlController;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.apps.gql.lucenesearch.LuceneService;
import no.nav.nom.apps.gql.resolvers.SearchResolver;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
import no.nav.nom.libs.state.ressurs.RessursStateRepositoryImpl;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockReset;

@Import({
     TestTokensGenerator.class
    , GraphQlController.class
    , SearchResolver.class

    , GraphQLConfig.class
    , GraphQLTestUtil.class

    , OrgenhetService.class
    , OrgEnhetStateRepositoryImpl.class
    , OrganiseringStateRepositoryImpl.class
    , RessursStateRepositoryImpl.class

    , LuceneService.class
    , DvhStartupLoadMonitor.class
})
public class GraphqlTestConfigSearch {
    @Bean
    DvhRepository dvhRepository() { return Mockito.mock(DvhRepository.class, MockReset.after()); }
    @Bean
    OrgEnhetCommandService orgEnhetCommandService() { return Mockito.mock(OrgEnhetCommandService.class, MockReset.after()); }
    @Bean
    KafkaTopicReader kafkaTopicReader() { return Mockito.mock(KafkaTopicReader.class, MockReset.after()); }
    @Bean
    GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter() { return Mockito.mock(GraphQLFieldConsumptionCounter.class, MockReset.after()); }
}
