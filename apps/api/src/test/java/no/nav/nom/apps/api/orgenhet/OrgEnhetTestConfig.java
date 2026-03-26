package no.nav.nom.apps.api.orgenhet;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.DvhOrgEnhetMockUtils;
import no.nav.nom.apps.api.KafkaUtils;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsClientImpl;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.EmbeddeKafkaAdminClient;
import no.nav.nom.apps.api.common.LocalConfiguration;
import no.nav.nom.apps.api.common.testidenter.ManuelleRessurserOgLedereTilState;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.dvh.DvhService;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.dvh.KontrollerFlyttingAvKontaktsenter;
import no.nav.nom.apps.api.organisering.OrganiseringService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetEventKafkaConfig;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetEventProducer;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetIdGenerator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.recoverybackup.OrgEnhetEventSourceRepository;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetsLast;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.ValidationRepository;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.ValidationService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.dvh.EksterneRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.orgtilknytning.job.OTUtenforHierarki;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.implementations.dvh.RessursHendelseForEventsourceService;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.libs.gql.lucenesearch.LuceneService;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.test.context.bean.override.mockito.MockReset;

@Import({
    DvhOrgEnhetMockUtils.class
    , DvhRestConsumer.class
    , DvhService.class
    , DvhOrgenhetsLast.class
    , DvhOrdsService.class
    , ValidationService.class
    , NomProxyClient.class
    , OrgenhetService.class
    , OrgEnhetCommandService.class
    , OrgEnhetEventProducer.class
    , OrgEnhetEventKafkaConfig.class
    , OrgEnhetIdGenerator.class
    , OrgEnhetTypeHandler.class
    , OrgEnhetOverordnetResolver.class
    , OrgEnhetUnderordnetResolver.class
    , OrgEnhetNomNivaaHandler.class
    , DvhStartupLoadMonitor.class
    , KafkaUtils.class
    , KafkaAutoConfiguration.class
    , EmbeddeKafkaAdminClient.class
    , LocalConfiguration.class
    , LeaderElection.class
    , OTUtenforHierarki.class
    , OrgtilknytningService.class
    , DvhOrdsClientImpl.class
    , WebClientAutoConfiguration.class
    , KontrollerFlyttingAvKontaktsenter.class
})
@EnableConfigurationProperties(value = AppProperties.class)
@EnableKafka
@AutoConfigureJson
public class OrgEnhetTestConfig {
    @Bean
    ValidationRepository validationRepository() {
        return Mockito.mock(ValidationRepository.class, MockReset.after());
    }

    @Bean
    RessursStateRepository ressursStateRepository() {
        return Mockito.mock(RessursStateRepository.class, MockReset.after());
    }

    @Bean
    OrgtilknytningStateRepository orgtilknytningStateRepository() {
        return Mockito.mock(OrgtilknytningStateRepository.class, MockReset.after());
    }

    @Bean
    RessursHendelseForEventsourceService ressursHendelseForEventsourceService() {
        return Mockito.mock(RessursHendelseForEventsourceService.class, MockReset.after());
    }

    @Bean
    KafkaTopicReader kafkaTopicReader() {
        return Mockito.mock(KafkaTopicReader.class, MockReset.after());
    }

    @Bean
    RessursStillingLoader ressursStillingLoader() {
        return Mockito.mock(RessursStillingLoader.class, MockReset.after());
    }

    @Bean
    EksterneRessurserLoader eksterneRessurserLoader() {
        return Mockito.mock(EksterneRessurserLoader.class, MockReset.after());
    }

    @Bean
    OrganiseringService organiseringService() {
        return Mockito.mock(OrganiseringService.class, MockReset.after());
    }

    @Bean
    AuthorizedNavident authorizedNavident() {
        return Mockito.mock(AuthorizedNavident.class, MockReset.after());
    }

    @Bean
    OrgEnhetEventSourceRepository orgEnhetEventSourceRepository() {
        return Mockito.mock(OrgEnhetEventSourceRepository.class, MockReset.after());
    }

    @Bean
    NomMastered nomMastered() {
        return Mockito.mock(NomMastered.class, MockReset.after());
    }

    @Bean
    LuceneService luceneService() {
        return Mockito.mock(LuceneService.class, MockReset.after());
    }

    @Bean
    Unleash unleash() {
        return Mockito.mock(Unleash.class, MockReset.after());
    }

    @Bean
    ManuelleRessurserOgLedereTilState manuelleRessurserOgLedereTilState() {
        return Mockito.mock(ManuelleRessurserOgLedereTilState.class, MockReset.after());
    }
}

