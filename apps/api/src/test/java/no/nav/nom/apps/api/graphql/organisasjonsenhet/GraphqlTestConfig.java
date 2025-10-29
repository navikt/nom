package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.GraphQLTestUtil;
import no.nav.nom.apps.api.graphql.common.config.GraphQLConfig;
import no.nav.nom.apps.api.graphql.common.metrics.GraphQLFieldConsumptionCounter;
import no.nav.nom.apps.api.graphql.controller.GraphQLOrgEnhetController;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepositoryImpl;
import no.nav.nom.libs.state.ressurs.RessursStateRepositoryImpl;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockReset;

@Import({
     TestTokensGenerator.class
    , GraphQLOrgEnhetController.class
    , OrgEnhetResolver.class
    , OrgtilknytningResolver.class
    , OrganiseringResolver.class
    , LederResolver.class
    , RemedyEnhetIdResolver.class

    , GraphQLConfig.class
    , GraphQLTestUtil.class

    , OrgenhetService.class
    , OrgEnhetStateRepositoryImpl.class
    , OrganiseringStateRepositoryImpl.class
    , OrgtilknytningStateRepositoryImpl.class
    , RessursStateRepositoryImpl.class
    , OrgtilknytningService.class
})
public class GraphqlTestConfig {
    @Bean
    DvhRepository dvhRepository() { return Mockito.mock(DvhRepository.class, MockReset.after()); }
    @Bean
    OrgEnhetCommandService orgEnhetCommandService() { return Mockito.mock(OrgEnhetCommandService.class, MockReset.after()); }
    @Bean
    KafkaTopicReader kafkaTopicReader() { return Mockito.mock(KafkaTopicReader.class, MockReset.after()); }
    @Bean
    RemedyEnhetMapperService remedyEnhetMapperService() { return Mockito.mock(RemedyEnhetMapperService.class, MockReset.after()); }
    @Bean
    GraphQLFieldConsumptionCounter graphQLFieldConsumptionCounter() { return Mockito.mock(GraphQLFieldConsumptionCounter.class, MockReset.after()); }
}
